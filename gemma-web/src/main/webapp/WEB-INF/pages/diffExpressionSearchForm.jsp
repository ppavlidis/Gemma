<%-- 
author: keshav
version: $Id$
--%>
<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="diffExpressionSearch.title" /></title>

	<jwr:script src='/scripts/ajax/diff/DiffExpressionSearchForm.js' />
	<jwr:script src='/scripts/app/DiffExpressionSearch.js' />

	<content tag="heading">
	<fmt:message key="diffExpressionSearch.title" />
	</content>

</head>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<div id='diffExpression-messages' style='width: 100%; height: 1.2em; margin: 5px'></div>
<div id='diffExpression-form' style='width: 500px; margin-bottom: 1em;'></div>
<div id='diffExpression-results'></div>

