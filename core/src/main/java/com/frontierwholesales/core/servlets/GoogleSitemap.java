
package com.frontierwholesales.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.day.cq.commons.Externalizer;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
@Component(metatype = true, label = "Google - Site Map Servlet", description = "Page and Asset Site Map Servlet", configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@SuppressWarnings("serial")
@Properties({
        @Property(name = "sling.servlet.resourceTypes", unbounded = PropertyUnbounded.ARRAY, label = "Sling Resource Type", description = "Sling Resource Type for the Home Page component or components."),
        @Property(name = "sling.servlet.selectors", value = "sitemaps", propertyPrivate = true),
        @Property(name = "sling.servlet.extensions", value = "xml", propertyPrivate = true),
        @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true),
        @Property(name = "webconsole.configurationFactory.nameHint", value = "Site Map for: {externalizer.domain}, on resource types: [{sling.servlet.resourceTypes}]") })

public final class GoogleSitemap extends SlingSafeMethodsServlet {
	

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");

    private static final boolean DEFAULT_INCLUDE_LAST_MODIFIED = false;

    private static final boolean DEFAULT_INCLUDE_INHERITANCE_VALUE = false;

    private static final String DEFAULT_EXTERNALIZER_DOMAIN = "author";

    private static final boolean DEFAULT_EXTENSIONLESS_URLS = false;

    @Property(value = DEFAULT_EXTERNALIZER_DOMAIN, label = "Externalizer Domain", description = "Must correspond to a configuration of the Externalizer component.")
    private static final String PROP_EXTERNALIZER_DOMAIN = "externalizer.domain";

    @Property(boolValue = DEFAULT_INCLUDE_LAST_MODIFIED, label = "Include Last Modified", description = "If true, the last modified value will be included in the sitemap.")
    private static final String PROP_INCLUDE_LAST_MODIFIED = "include.lastmod";

    @Property(label = "Change Frequency Properties", unbounded = PropertyUnbounded.ARRAY, description = "The set of JCR property names which will contain the change frequency value.")
    private static final String PROP_CHANGE_FREQUENCY_PROPERTIES = "changefreq.properties";

    @Property(label = "Priority Properties", unbounded = PropertyUnbounded.ARRAY, description = "The set of JCR property names which will contain the priority value.")
    private static final String PROP_PRIORITY_PROPERTIES = "priority.properties";

    @Property(label = "DAM Folder Property", description = "The JCR property name which will contain DAM folders to include in the sitemap.")
    //overriding so that it does not try to pull the assets from the page. only the dam.
  
    private static final String PROP_DAM_ASSETS_PROPERTY = "/content/dam/frontierwholesales";

    @Property(label = "DAM Asset MIME Types", unbounded = PropertyUnbounded.ARRAY, description = "MIME types allowed for DAM assets.")
    private static final String PROP_DAM_ASSETS_TYPES = "damassets.types";

    @Property(label = "Exclude from Sitemap Property", description = "The boolean [cq:Page]/jcr:content property name which indicates if the Page should be hidden from the Sitemap. Default value: hideInNav")
    private static final String PROP_EXCLUDE_FROM_SITEMAP_PROPERTY = "exclude.property";

    @Property(boolValue = DEFAULT_INCLUDE_INHERITANCE_VALUE, label = "Include Inherit Value", description = "If true searches for the frequency and priority attribute in the current page if null looks in the parent.")
    private static final String PROP_INCLUDE_INHERITANCE_VALUE = "include.inherit";

    @Property(boolValue = DEFAULT_EXTENSIONLESS_URLS, label = "Extensionless URLs", description = "If true, page links included in sitemap are generated without .html extension and the path is included with a trailing slash, e.g. /content/geometrixx/en/.")
    private static final String PROP_EXTENSIONLESS_URLS = "extensionless.urls";

    @Property(label = "Character Encoding", description = "If not set, the container's default is used (ISO-8859-1 for Jetty)")
    private static final String PROP_CHARACTER_ENCODING_PROPERTY = "character.encoding";

    private static final String NS = "http://www.sitemaps.org/schemas/sitemap/0.9";

    @Reference
    private Externalizer externalizer;

    private String externalizerDomain;

    private boolean includeInheritValue;

    private boolean includeLastModified;

    private String[] changefreqProperties;

    private String[] priorityProperties;


    private List<String> damAssetTypes;

    private String excludeFromSiteMapProperty;

    private String characterEncoding;

    private boolean extensionlessUrls;

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.externalizerDomain = PropertiesUtil.toString(properties.get(PROP_EXTERNALIZER_DOMAIN),
                DEFAULT_EXTERNALIZER_DOMAIN);
        this.includeLastModified = PropertiesUtil.toBoolean(properties.get(PROP_INCLUDE_LAST_MODIFIED),
                DEFAULT_INCLUDE_LAST_MODIFIED);
        this.includeInheritValue = PropertiesUtil.toBoolean(properties.get(PROP_INCLUDE_INHERITANCE_VALUE),
                DEFAULT_INCLUDE_INHERITANCE_VALUE);
        this.changefreqProperties = PropertiesUtil.toStringArray(properties.get(PROP_CHANGE_FREQUENCY_PROPERTIES),
                new String[0]);
        this.priorityProperties = PropertiesUtil.toStringArray(properties.get(PROP_PRIORITY_PROPERTIES), new String[0]);
        
        this.damAssetTypes = Arrays
                .asList(PropertiesUtil.toStringArray(properties.get(PROP_DAM_ASSETS_TYPES), new String[0]));
        this.excludeFromSiteMapProperty = PropertiesUtil.toString(properties.get(PROP_EXCLUDE_FROM_SITEMAP_PROPERTY),
                NameConstants.PN_HIDE_IN_NAV);
        this.characterEncoding = PropertiesUtil.toString(properties.get(PROP_CHARACTER_ENCODING_PROPERTY), null);
        this.extensionlessUrls = PropertiesUtil.toBoolean(properties.get(PROP_EXTENSIONLESS_URLS),
                DEFAULT_EXTENSIONLESS_URLS);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType(request.getResponseContentType());
        if (StringUtils.isNotEmpty(this.characterEncoding)) {
            response.setCharacterEncoding(characterEncoding);
        }
        ResourceResolver resourceResolver = request.getResourceResolver();
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());

        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        try {
            XMLStreamWriter stream = outputFactory.createXMLStreamWriter(response.getWriter());
            stream.writeStartDocument("1.0");

            stream.writeStartElement("", "urlset", NS);
            stream.writeNamespace("", NS);

            // first do the current page
            write(page, stream, resourceResolver);

            for (Iterator<Page> children = page.listChildren(new PageFilter(false, true), true); children.hasNext();) {
                write(children.next(), stream, resourceResolver);
            }
                       
                Resource rootRes = resourceResolver.getResource(PROP_DAM_ASSETS_PROPERTY);   
                for (Iterator<Resource> children = rootRes.listChildren(); children.hasNext();) {
                    Resource assetFolderChild = children.next();
                    if (assetFolderChild.isResourceType(DamConstants.NT_DAM_ASSET)) {
                        Asset asset = assetFolderChild.adaptTo(Asset.class);
                       
                        if (damAssetTypes.contains(asset.getMimeType())) {
                            writeAsset(asset, stream, resourceResolver);
                        }
                    } else {
                        writeAssets(stream, assetFolderChild, resourceResolver);
                    }
                }

            stream.writeEndElement();

            stream.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

  
    @SuppressWarnings("squid:S1192")
    private void write(Page page, XMLStreamWriter stream, ResourceResolver resolver) throws XMLStreamException {
        if (isHidden(page)) {
            return;
        }
        stream.writeStartElement(NS, "url");
        String loc = "";

        if (!extensionlessUrls) {
            loc = externalizer.externalLink(resolver, externalizerDomain, String.format("%s.html", page.getPath()));
        } else {
            loc = externalizer.externalLink(resolver, externalizerDomain, String.format("%s/", page.getPath()));
        }

        writeElement(stream, "loc", loc);

        if (includeLastModified) {
            Calendar cal = page.getLastModified();
            if (cal != null) {
                writeElement(stream, "lastmod", DATE_FORMAT.format(cal));
            }
        }

        if (includeInheritValue) {
            HierarchyNodeInheritanceValueMap hierarchyNodeInheritanceValueMap = new HierarchyNodeInheritanceValueMap(
                    page.getContentResource());
            writeFirstPropertyValue(stream, "changefreq", changefreqProperties, hierarchyNodeInheritanceValueMap);
            writeFirstPropertyValue(stream, "priority", priorityProperties, hierarchyNodeInheritanceValueMap);
        } else {
            ValueMap properties = page.getProperties();
            writeFirstPropertyValue(stream, "changefreq", changefreqProperties, properties);
            writeFirstPropertyValue(stream, "priority", priorityProperties, properties);
        }

        stream.writeEndElement();
    }

    private boolean isHidden(final Page page) {
        return page.getProperties().get(this.excludeFromSiteMapProperty, false);
    }

    private void writeAsset(Asset asset, XMLStreamWriter stream, ResourceResolver resolver) throws XMLStreamException {
        stream.writeStartElement(NS, "url");

        String loc = externalizer.externalLink(resolver, externalizerDomain, asset.getPath());
        writeElement(stream, "loc", loc);

        if (includeLastModified) {
            long lastModified = asset.getLastModified();
            if (lastModified > 0) {
                writeElement(stream, "lastmod", DATE_FORMAT.format(lastModified));
            }
        }

        Resource contentResource = asset.adaptTo(Resource.class).getChild(JcrConstants.JCR_CONTENT);
        if (contentResource != null) {
            if (includeInheritValue) {
                HierarchyNodeInheritanceValueMap hierarchyNodeInheritanceValueMap = new HierarchyNodeInheritanceValueMap(
                        contentResource);
                writeFirstPropertyValue(stream, "changefreq", changefreqProperties, hierarchyNodeInheritanceValueMap);
                writeFirstPropertyValue(stream, "priority", priorityProperties, hierarchyNodeInheritanceValueMap);
            } else {
                ValueMap properties = contentResource.getValueMap();
                writeFirstPropertyValue(stream, "changefreq", changefreqProperties, properties);
                writeFirstPropertyValue(stream, "priority", priorityProperties, properties);
            }
        }

        stream.writeEndElement();
    }

    private void writeAssets(final XMLStreamWriter stream, final Resource assetFolder, final ResourceResolver resolver)
            throws XMLStreamException {
        for (Iterator<Resource> children = assetFolder.listChildren(); children.hasNext();) {
            Resource assetFolderChild = children.next();
            if (assetFolderChild.isResourceType(DamConstants.NT_DAM_ASSET)) {
                Asset asset = assetFolderChild.adaptTo(Asset.class);
               
               if (damAssetTypes.contains(asset.getMimeType())) {
                    writeAsset(asset, stream, resolver);
                }
            } else {
                writeAssets(stream, assetFolderChild, resolver);
            }
        }
    }

    private void writeFirstPropertyValue(final XMLStreamWriter stream, final String elementName,
            final String[] propertyNames, final ValueMap properties) throws XMLStreamException {
        for (String prop : propertyNames) {
            String value = properties.get(prop, String.class);
            if (value != null) {
                writeElement(stream, elementName, value);
                break;
            }
        }
    }

    @SuppressWarnings("squid:S1144")
    private void writeFirstPropertyValue(final XMLStreamWriter stream, final String elementName,
            final String[] propertyNames, final InheritanceValueMap properties) throws XMLStreamException {
        for (String prop : propertyNames) {
            String value = properties.get(prop, String.class);
            if (value == null) {
                value = properties.getInherited(prop, String.class);
            }
            if (value != null) {
                writeElement(stream, elementName, value);
                break;
            }
        }
    }

    private void writeElement(final XMLStreamWriter stream, final String elementName, final String text)
            throws XMLStreamException {
        stream.writeStartElement(NS, elementName);
        stream.writeCharacters(text);
        stream.writeEndElement();
    }

}

