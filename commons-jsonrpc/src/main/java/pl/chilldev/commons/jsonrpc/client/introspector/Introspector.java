/**
 * This file is part of the ChillDev-Commons.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2015 - 2016 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.chilldev.commons.jsonrpc.client.introspector;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.chilldev.commons.jsonrpc.client.ClientModule;
import pl.chilldev.commons.jsonrpc.client.Connector;
import pl.chilldev.commons.jsonrpc.rpc.introspector.JsonRpcCall;
import pl.chilldev.commons.jsonrpc.rpc.introspector.JsonRpcParam;

/**
 * Introspector for clients classes to automatically map method to JSON calls.
 */
public class Introspector
{
    /**
     * Wrapper for parameter mapper that reduces dependency to just call parameters.
     *
     * @param <Type> Mapping parameter type.
     */
    @FunctionalInterface
    interface ParameterMapperWrapper<Type>
    {
        /**
         * Populates call parameters with given value.
         *
         * @param value Parameter value passed to the call.
         * @param params Current state of RPC call parameters.
         */
        void putParam(Type value, Map<String, Object> params);
    }

    /**
     * RPC method call.
     *
     * @param <Type> Result type.
     */
    static class Call<Type>
    {
        /**
         * RPC method name.
         */
        private String name;

        /**
         * Parameters mappers.
         */
        private List<Introspector.ParameterMapperWrapper<Object>> params;

        /**
         * Response handler.
         */
        private Function<Object, ? extends Type> handler;

        /**
         * Initializes RPC call handler.
         *
         * @param name Request method name.
         * @param params Parameters mappers.
         * @param handler Response handler.
         */
        Call(
            String name,
            List<Introspector.ParameterMapperWrapper<Object>> params,
            Function<Object, ? extends Type> handler
        )
        {
            this.name = name;
            this.params = params;
            this.handler = handler;
        }

        /**
         * Executes request on given connector.
         *
         * @param connector TCP connector.
         * @param arguments Request parameters.
         * @return Response result.
         */
        public Type execute(Connector connector, Object[] arguments)
        {
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < arguments.length; ++i) {
                this.params.get(i).putParam(arguments[i], params);
            }

            return this.handler.apply(
                params.isEmpty()
                    ? connector.execute(this.name)
                    : connector.execute(this.name, params)
            );
        }
    }

    /**
     * RPC calls wrapper.
     */
    public static class Client
    {
        /**
         * TCP connector.
         */
        private Connector connector;

        /**
         * RPC calls.
         */
        private Map<Method, Introspector.Call<?>> calls = new HashMap<>();

        /**
         * Initializes service over given client.
         *
         * @param connector TCP connector.
         */
        Client(Connector connector)
        {
            this.connector = connector;
        }

        /**
         * Registers method call handler.
         *
         * @param method Client class method.
         * @param call RPC call handler.
         */
        public void register(Method method, Introspector.Call<?> call)
        {
            this.calls.put(method, call);
        }

        /**
         * Executes the RPC call.
         *
         * @param method Invoked method.
         * @param arguments Call-time arguments.
         * @return Execution result.
         */
        @RuntimeType
        public Object execute(@Origin Method method, @AllArguments Object[] arguments)
        {
            return this.calls.get(method).execute(this.connector, arguments);
        }
    }

    /**
     * Default parameter mapper.
     */
    private static final ParameterMapper<Object> DEFAULT_MAPPER
        = (String name, Object value, Map<String, Object> params) -> params.put(name, value);

    /**
     * Transparent response handler.
     */
    private static final Function<Object, ?> IDENTITY_HANDLER = (Object value) -> value;

    /**
     * Client modules SPIs.
     */
    private static Set<ClientModule> modules = new HashSet<>();

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(Introspector.class);

    /**
     * Parameters mappers.
     */
    private Map<Class<?>, ParameterMapper<?>> mappers = new HashMap<>();

    /**
     * Results handlers.
     */
    private Map<Class<?>, Function<Object, ?>> handlers = new HashMap<>();

    static {
        ServiceLoader<ClientModule> loader = ServiceLoader.load(ClientModule.class);
        loader.forEach(Introspector.modules::add);
    }

    /**
     * Registers parameter mapper for given class.
     *
     * @param type Parameter type.
     * @param mapper Parameter mapper.
     * @param <Type> Parameter type.
     */
    public <Type> void registerParameterMapper(
        Class<Type> type,
        ParameterMapper<? super Type> mapper
    )
    {
        this.mappers.put(
            type,
            mapper
        );
    }

    /**
     * Registers response type handler for given class.
     *
     * @param type Response type.
     * @param handler Response handler.
     * @param <Type> Response object type.
     */
    public <Type> void registerResultHandler(
        Class<Type> type,
        Function<Object, ? extends Type> handler
    )
    {
        this.handlers.put(
            type,
            handler
        );
    }

    /**
     * Builds client wrapper that handles all of the JSON-RPC calls.
     *
     * @param type Base client type.
     * @param connector TPC connector to use for the calls.
     * @param <Type> Client type.
     * @return Client instance.
     */
    public <Type> Class<? extends Type> createClient(Class<Type> type, Connector connector)
    {
        return new ByteBuddy()
            .subclass(type)
            .method(ElementMatchers.isAnnotatedWith(JsonRpcCall.class))
            .intercept(MethodDelegation.to(this.buildClient(type, connector)))
            .make()
            .load(this.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
            .getLoaded();
    }

    /**
     * Creates RPC client.
     *
     * @param type Base client type.
     * @param connector TPC connector to use for the calls.
     * @return RPC client.
     */
    private Introspector.Client buildClient(Class<?> type, Connector connector)
    {
        Introspector.Client client = new Introspector.Client(connector);

        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(JsonRpcCall.class)) {
                this.logger.debug("Found {}.{} method as JSON-RPC request.", type.getName(), method.getName());

                JsonRpcCall call = method.getAnnotation(JsonRpcCall.class);

                Parameter[] parameters = method.getParameters();
                List<Introspector.ParameterMapperWrapper<Object>> mappers
                    = new ArrayList<>(parameters.length);

                // build parameters resolvers
                for (Parameter parameter : parameters) {
                    // register synthetic RPC handler
                    mappers.add(this.createParameterMapper(parameter));
                }

                // response handler
                Class<?> response = method.getReturnType();
                Function<Object, ?> handler = this.handlers.containsKey(response)
                    ? this.handlers.get(response)
                    : Introspector.IDENTITY_HANDLER;

                client.register(
                    // use overridden name if set
                    method,
                    new Introspector.Call<>(
                        call.name().isEmpty() ? method.getName() : call.name(),
                        mappers,
                        handler
                    )
                );
            }
        }

        return client;
    }

    /**
     * Creates mapper for given parameter.
     *
     * @param parameter Method parameter.
     * @param <Type> Parameter type.
     * @return Parameter provider.
     */
    private <Type> Introspector.ParameterMapperWrapper<Type> createParameterMapper(Parameter parameter)
    {
        // try to fetch provider by parameter type
        @SuppressWarnings("unchecked")
        Class<Type> type = (Class<Type>) parameter.getType();
        @SuppressWarnings("unchecked")
        ParameterMapper<? super Type> mapper = this.mappers.containsKey(type)
            ? (ParameterMapper<Type>) this.mappers.get(type)
            : Introspector.DEFAULT_MAPPER;

        String name = parameter.getName();

        // override defaults if annotation is defined
        JsonRpcParam metadata = parameter.getAnnotation(JsonRpcParam.class);
        if (metadata != null) {
            name = metadata.name().isEmpty() ? name : metadata.name();
        }

        return this.createParameterMapperWrapper(mapper, name);
    }

    /**
     * Creates parameter mapper wrapper for given parameter scope.
     *
     * @param mapper Value mapper.
     * @param name Parameter name.
     * @param <Type> Parameter type.
     * @return Wrapped parameter mapper.
     */
    private <Type> Introspector.ParameterMapperWrapper<Type> createParameterMapperWrapper(
        ParameterMapper<? super Type> mapper,
        String name
    )
    {
        return (Type value, Map<String, Object> params) -> mapper.putParam(name, value, params);
    }

    /**
     * Creates introspector initializes with SPI services.
     *
     * @return Introspector.
     */
    public static Introspector createDefault()
    {
        Introspector introspector = new Introspector();

        Introspector.modules.forEach((ClientModule module) -> module.initializeIntrospector(introspector));

        return introspector;
    }
}
