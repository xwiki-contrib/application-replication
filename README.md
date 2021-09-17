# Replication Application

Various tools to replicate data between XWiki instances.

* Project Lead: [Thomas Mortagne](http://www.xwiki.org/xwiki/bin/view/XWiki/ThomasMortagne)
* [Documentation & Downloads](https://extensions.xwiki.org/xwiki/bin/view/Extension/Replication%20Application)
* [Issue Tracker](http://jira.xwiki.org/browse/REPLICAT)
* Communication: [Forum](https://forum.xwiki.org), [Chat](https://dev.xwiki.org/xwiki/bin/view/Community/Chat)
* [Development Practices](http://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 13.17
* License: LGPL 2.1
* Translations: https://l10n.xwiki.org/projects/xwiki-contrib/application-replication-default/
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](https://ci.xwiki.org/buildStatus/icon?job=XWiki+Contrib%2Fapplication-replication%2Fmaster)](https://ci.xwiki.org/job/XWiki%20Contrib/job/application-replication/job/master/)

# Release

* Release

```
mvn release:prepare -Pintegration-tests
mvn release:perform -Pintegration-tests
```
