/**
 * This file is part of the ChillDev-Commons.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2015 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.chilldev.commons.jsonrpc.json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * JSON data conversion utilities.
 */
public class ConvertUtils
{
    /**
     * Builds paged response.
     *
     * @param result JSON-RPC call response.
     * @param request Page request specification.
     * @param type Desired type class.
     * @param recordsParam Page content parameter name.
     * @param countParam Results count parameter name.
     * @param <Type> Contained type.
     * @return Paged response.
     */
    public static <Type> Page<Type> buildPage(
        Object result,
        Pageable request,
        Class<Type> type,
        String recordsParam,
        String countParam
    )
    {
        // verify if the result is a params container
        if (!(result instanceof Map)) {
            throw new ClassCastException(
                String.format(
                    "Result returned from server is not a dictionary of returned value: %s found.",
                    result.getClass().getName()
                )
            );
        }
        Map<?, ?> params = (Map<?, ?>) result;

        result = params.get(recordsParam);
        if (!(result instanceof List)) {
            throw new ClassCastException(
                String.format(
                    "Value returned under key \"%s\" is not a list of records: %s found.",
                    recordsParam,
                    result.getClass().getName()
                )
            );
        }
        List<?> records = (List<?>) result;

        // convert all elements
        List<Type> list = records.stream()
                .map((Object record) -> ParamsRetriever.OBJECT_MAPPER.convertValue(record, type))
                .collect(Collectors.toList());

        return new PageImpl<Type>(
            list,
            request,
            Long.parseLong(params.get(countParam).toString())
        );
    }

    /**
     * Builds paged response for the default parameters names (`{"records":[…],"count":X}`).
     *
     * @param result JSON-RPC call response.
     * @param request Page request specification.
     * @param type Desired type class.
     * @param <Type> Contained type.
     * @return Paged response.
     */
    public static <Type> Page<Type> buildPage(Object result, Pageable request, Class<Type> type)
    {
        return ConvertUtils.buildPage(
            result,
            request,
            type,
            ParamsRetriever.DEFAULTPARAM_RECORDS,
            ParamsRetriever.DEFAULTPARAM_COUNT
        );
    }
}
