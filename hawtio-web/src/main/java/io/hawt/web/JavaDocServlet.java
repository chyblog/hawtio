/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hawt.web;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * A servlet for returning the javadoc files for a given set of maven coordinates and file paths
 */
public class JavaDocServlet extends HttpServlet {
    private MBeanServer mbeanServer;
    private ObjectName objectName;
    private String[] argumentTypes = {"java.lang.String", "java.lang.String"};

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        try {
            if (mbeanServer == null) {
                mbeanServer = ManagementFactory.getPlatformMBeanServer();
            }
            if (objectName == null) {
                objectName = new ObjectName("org.fusesource.insight:type=LogQuery");
            }
        } catch (MalformedObjectNameException e) {
            throw new ServletException("Failed to initialise LogQuery MBean: " + e, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (mbeanServer != null && objectName != null) {
            while (pathInfo.startsWith("/")) {
                pathInfo = pathInfo.substring(1);
            }
            int idx = pathInfo.indexOf('/');
            if (idx > 0) {
                String mavenCoords = pathInfo.substring(0, idx);
                String path = pathInfo.substring(idx + 1);
                if (path == null || path.trim().length() == 0) {
                    path = "index.html";
                }
                Object[] arguments = {mavenCoords, path};
                try {
                    Object answer = mbeanServer.invoke(objectName, "getJavaDoc", arguments, argumentTypes);
                    if (answer instanceof String) {
                        if (!pathInfo.endsWith(".css")) {
                            resp.setContentType("text/html;charset=utf-8");
                        }
                        resp.getWriter().println(answer);
                    }
                } catch (Exception e) {
                    throw new ServletException("Failed to find javadoc from maven coordinates " + mavenCoords + " path " + path + ". Reason " + e, e);
                }
            }
        }
    }
}
