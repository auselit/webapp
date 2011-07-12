package net.metadata.auselit.views;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import junit.framework.TestCase;
import au.edu.diasb.annotation.danno.DannoResponse;
import au.edu.diasb.annotation.danno.common.DannoProperties;
import au.edu.diasb.annotation.danno.impl.sesame.SesameAnnoteaTypeFactory;
import au.edu.diasb.annotation.danno.model.RDFContainer;
import au.edu.diasb.annotation.danno.test.MockHttpServletRequest;
import au.edu.diasb.annotation.danno.test.MockHttpServletResponse;
import au.edu.diasb.danno.constants.DannoMimeTypes;

public class DannoRSSViewTest extends TestCase {
    private SesameAnnoteaTypeFactory tf;
    
    private Properties props;
    
    @Override
    protected void setUp() throws Exception {
        tf = new SesameAnnoteaTypeFactory();
        props = new Properties();
        props.setProperty(DannoProperties.HOME_URL_PROP, "homeurl");
    }

	public void testRssView() throws Exception {
		DannoRSSView rssView = new DannoRSSView();
		rssView.setProperties(props);
		

        RDFContainer rdf = tf.createContainer(
                new ByteArrayInputStream(ANNOTATION_TEXT.getBytes()));
        DannoResponse mv = new DannoResponse(rdf, false, new Properties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
        request.addHeader("accept", DannoMimeTypes.XML_MIMETYPE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        mv.addObject(DannoResponse.RSS_FEED_TITLE_KEY, "feedtitle");
        
        rssView.render(mv.getModel(), request, response);
        
        assertNotNull(response.getContentAsString());
    }
	
    static final String ANNOTATION_TEXT = "<?xml version=\"1.0\" ?>\n" +
    "<r:RDF xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
    "       xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\"\n" +
    "       xmlns:d=\"http://purl.org/dc/elements/1.1/\">\n" +
    " <r:Description r:about=\"http://foo.bar\">\n" +
    "  <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\"/>\n" +
    "  <r:type r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\"/>\n" +
    "  <a:annotates r:resource=\"http://serv1.example.com/some/page.html\"/>\n" +
    "  <a:context>http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])</a:context>\n" +
    "  <d:title>Annotation of Sample Page</d:title>\n" +
    "  <d:creator>Ralph Swick</d:creator>\n" +
    "  <a:created>1999-10-14T12:10:01Z</a:created>\n" +
    "  <d:date>1999-10-14T12:10:01Z</d:date>\n" +
    "  <a:body r:resource=\"http://serv2.example.com/mycomment.html\"/>\n" +
    " </r:Description>\n" +
    "</r:RDF>\n";
}
