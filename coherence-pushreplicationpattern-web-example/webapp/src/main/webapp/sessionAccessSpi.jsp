<%@ page import="java.net.URLEncoder"%>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.io.IOException" %>

<%
String sId = request.getParameter("id");
String sUrl;
if (sId == null || sId.length() == 0)
    {
    sUrl = "sessionAccess.jsp";
    }
else
    {
    HttpSessionContext ctx = request.getSession(true).getSessionContext();
    session = ctx.getSession(sId);
    if (session == null)
        {
        session = request.getSession(true);
        sUrl    = "sessionAccess.jsp";
        }
    else
        {
        sUrl = "sessionAccess.jsp?id=" + URLEncoder.encode(sId);
        }
    }

if (request.getMethod().equalsIgnoreCase("POST"))
    {
    String sAction = request.getParameter("action");
    if (sAction != null)
        {
        String sKey   = request.getParameter("key");
        String sValue = request.getParameter("value");

        if (sAction.equals("add"))
            {
            if (request.getParameter("serializable") == null)
                {
                session.setAttribute(sKey, sValue);
                }
            else
                {
                session.setAttribute(sKey, sValue);
                }
            }
        else if (sAction.equals("remove"))
            {
            session.removeAttribute(sKey);
            }
        else if (sAction.equals("invalidate"))
            {
            session.invalidate();
            session = request.getSession(true);
            }
        }
    }
%>

<html>
  <head><title>Coherence*Web Push Replication Example</title></head>
  <body>
    <center>
      <h1>Coherence*Web Push Replication Example</h1>
    </center>
    <%
    printSessionInfo(session, out);
    %>
    <p/>
    <form id="HttpSessionAttributesForm" method="post" action="<%= response.encodeURL(sUrl) %>">
      <table border="1">
        <tr>
          <td>Key:</td>
          <td colspan="4"><input type="text" name="key" size="20"></td>
        </tr>
        <tr>
          <td>Value:</td>
          <td colspan="4"><input type="text" name="value" size="20"></td>
        </tr>
        <tr>
          <td>Serializable:</td>
          <td colspan="4"><input type="checkbox" name="serializable" checked="checked"></td>
        </tr>
        <tr>
          <td/>
          <td><input type="submit" name="action" value="add"></td>
          <td><input type="submit" name="action" value="remove"></td>
          <td><input type="submit" name="action" value="invalidate"></td>
          <td><input type="submit" name="action" value="refresh"></td>
        </tr>
      </table>
    </form>
  </body>
</html>

<%!
    public static void printSessionInfo(HttpSession session, JspWriter writer) {
        try {
        writer.println("    <h2>HttpSession Info:</h2>");
        writer.println("    <table id=\"HttpSessionInfo\" border=\"1\">");
        writer.println("      <tr>");
        writer.println("        <th>Property</th>");
        writer.println("        <th>Value</th>");
        writer.println("      </tr>");
        writer.println("      <tr>");
        writer.println("        <td>Class</td>");
        writer.println("        <td>");
        writer.print(session.getClass().getName());
        writer.println("</td>");
        writer.println("      </tr>");
        writer.println("      <tr>");
        writer.println("        <td>Id</td>");
        writer.println("        <td>");
        writer.print(session.getId());
        writer.println("</td>");
        writer.println("      </tr>");
        writer.println("      <tr>");
        writer.println("        <td>CreationTime</td>");
        writer.println("        <td>");
        writer.print(session.getCreationTime());
        writer.println("</td>");
        writer.println("      </tr>");
        writer.println("      <tr>");
        writer.println("        <td>LastAccessedTime</td>");
        writer.println("        <td>");
        writer.print(session.getLastAccessedTime());
        writer.println("</td>");
        writer.println("      </tr>");
        writer.println("      <tr>");
        writer.println("        <td>MaxInactiveInterval</td>");
        writer.println("        <td>");
        writer.print(session.getMaxInactiveInterval());
        writer.println("</td>");
        writer.println("      </tr>");
        writer.println("      <tr>");
        writer.println("        <td>New</td>");
        writer.println("        <td>");
        writer.print(session.isNew());
        writer.println("</td>");
        writer.println("      </tr>");
        writer.println("    </table>");
        writer.println("    <p/>");
        writer.println("    <h2>HttpSession Attributes:</h2>");
        writer.println("    <table id=\"HttpSessionAttributes\"border=\"1\">");
        writer.println("      <tr>");
        writer.println("        <th>Property</th>");
        writer.println("        <th>Value</th>");
        writer.println("      </tr>");

        for (Enumeration enmr = session.getAttributeNames(); enmr.hasMoreElements();) {
            String sName = (String) enmr.nextElement();
            writer.println("      <tr>");
            writer.println("        <td>");
            writer.print(sName);
            writer.println("</td>");
            writer.println("        <td>");
            writer.print(session.getAttribute(sName));
            writer.println("</td>");
            writer.println("      </tr>");
        }

        writer.println("    </table>");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
%>
