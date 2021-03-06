<!---
# This file is part of the ChillDev-Commons.
#
# @license http://mit-license.org/ The MIT license
# @copyright 2014 © by Rafał Wrzeszcz - Wrzasq.pl.
-->

# Git repository

All project sources and related resources are stored in [Git repository](https://github.com/chilloutdevelopment/pl.chilldev.commons.git). To standardize workflow with repository, *ChillDev-Commons* follows [Git-flow](http://nvie.com/posts/a-successful-git-branching-model/) policy. In order to synchronize your repository you should call `git flow init` in your repository directory with following settings:

-   _Branch name for production releases:_ - `master`
-   _Branch name for "next release" development:_ - `develop`
-   _Feature branches?_ - `feature/`
-   _Release branches?_ - `release/`
-   _Hotfix branches?_ - `hotfix/`
-   _Support branches?_ - `support/`
-   _Version tag prefix?_ - `release-`
