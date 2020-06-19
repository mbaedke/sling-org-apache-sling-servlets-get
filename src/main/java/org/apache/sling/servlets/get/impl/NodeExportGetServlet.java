/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.get.impl;

import java.io.IOException;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.jackrabbit.vault.fs.config.DefaultMetaInf;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.get.impl.helpers.Renderer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class,
        name="org.apache.sling.servlets.get.NodeExportGetServlet",
        property = {
                "service.description=Node Export GET Servlet",

                // Use this as a default servlet for Sling
                "sling.servlet.resourceTypes=sling/servlet/default",
                "sling.servlet.extensions=node",
                "sling.servlet.prefix:Integer=-1",

                // Generic handler for all get requests
                "sling.servlet.methods=GET",
                "sling.servlet.methods=HEAD"
        })
public class NodeExportGetServlet extends DefaultGetServlet {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private Packaging packaging;

    @Activate
    protected void activate(Config cfg) {
        logger.info("NodeExportGetServlet activated");
    }

    @Deactivate
    protected void deactivate() {
        logger.info("NodeExportGetServlet deactivated");
    }

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response) throws ServletException, IOException {

        if (ResourceUtil.isNonExistingResource(request.getResource())) {
            throw new ResourceNotFoundException(
                    request.getResource().getPath(), "No resource found");
        }

        Renderer renderer = new Renderer() {
            public void render(SlingHttpServletRequest req,
                               SlingHttpServletResponse resp) throws IOException {
                Resource r = req.getResource();
                if (ResourceUtil.isNonExistingResource(r)) {
                    throw new ResourceNotFoundException("No data to render.");
                }
                resp.setContentType(req.getResponseContentType());
                resp.setCharacterEncoding("UTF-8");

                Session session = req.getResourceResolver().adaptTo(Session.class);
                JcrPackageManager jcrPackageManager = packaging.getPackageManager(session);
                ExportOptions exportOptions = new ExportOptions();
                exportOptions.setRootPath(r.getPath());
                exportOptions.setMountPath(r.getPath());
                DefaultMetaInf metaInf = new DefaultMetaInf();
                Properties props = new Properties();
                props.put(PackageProperties.NAME_GROUP, "");
                props.put(PackageProperties.NAME_NAME, "");
                metaInf.setProperties(props);
                exportOptions.setMetaInf(metaInf);
                exportOptions.setNodeOnly(true);
                try {
                    jcrPackageManager.assemble(session, exportOptions, resp.getOutputStream());
                } catch (RepositoryException e) {
                    throw new SlingException("Failed to create package", e);
                }
            }
        };
        renderer.render(request, response);
    }
}
