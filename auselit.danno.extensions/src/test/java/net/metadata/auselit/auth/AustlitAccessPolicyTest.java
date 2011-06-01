package net.metadata.auselit.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;

import au.edu.diasb.annotation.danno.DannoAccessPolicy;
import au.edu.diasb.annotation.danno.DannoIdentityProvider;
import au.edu.diasb.annotation.danno.DefaultAccessPolicy;
import au.edu.diasb.annotation.danno.impl.jena.JenaAnnoteaTypeFactory;
import au.edu.diasb.annotation.danno.model.RDFContainer;
import au.edu.diasb.annotation.danno.model.RDFObject;
import au.edu.diasb.annotation.danno.model.RDFValue;
import au.edu.diasb.annotation.danno.test.MockAuthenticationContext;
import au.edu.diasb.chico.mvc.RequestFailureException;

public class AustlitAccessPolicyTest extends TestCase {


	private RDFObject privateAnnoteaObject;
	private RDFObject publicAnnoteaObject;
	private RDFObject unmarkedPublicAnnotation;
	private RDFObject httpMessage;
	private HttpServletRequest request;
	private RDFContainer multiAnnoContainer;
	
	public void setUp() throws Exception {
		privateAnnoteaObject = getSampleRDFObject(getPrivateAnnoString());
		publicAnnoteaObject = getSampleRDFObject(getPublicAnnoString());
		unmarkedPublicAnnotation = getSampleRDFObject(getUnmarkedPublicAnnoString());
		httpMessage = getSampleRDFObject(getHttpMessageString());
		request = new MockHttpServletRequest();
		multiAnnoContainer = createMultiAnnoContainer();
	}
	
	public void testSingleRDFContainer() throws Exception {	
		RDFValue property = privateAnnoteaObject
				.getProperty("http://auselit.metadata.net/privanno/private");
		assertTrue(property.isLiteral());
		String prop = privateAnnoteaObject
				.getLiteralProperty("http://auselit.metadata.net/privanno/private");
		assertEquals("true", prop);
	}
	
	public void testMultipleRDFContainer() throws Exception {
		RDFContainer container = createMultiAnnoContainer();
		
		assertEquals(2, container.getRDFObjectURIs().size());
	}

	private RDFContainer createMultiAnnoContainer() {
		JenaAnnoteaTypeFactory typeFactory = new JenaAnnoteaTypeFactory();
		RDFContainer container = typeFactory.createContainer();
		container.addAll(privateAnnoteaObject);
		container.addAll(publicAnnoteaObject);
		return container;
	}

	public void testDefaultDannoAccessPolicy() throws RequestFailureException {
		DefaultAccessPolicy accessPolicy = new DefaultAccessPolicy();
		String[] authorities = {"ROLE_USER"};
		accessPolicy.setAuthenticationContext(new MockAuthenticationContext(
				"name", "id", authorities));

		RDFContainer res = new MockRDFContainer();

		accessPolicy.checkRead(request, res);

	}

	public void testCheckReadAllowed() throws Exception {
		DannoIdentityProvider ip = new MockDannoIdentityProvider("exampleowner");
		DannoAccessPolicy accessPolicy = getAustlitAccessPolicy(ip);

		accessPolicy.checkRead(request, privateAnnoteaObject);
	}

	
	public void testCheckReadFail() throws Exception {
		DannoIdentityProvider ip = new MockDannoIdentityProvider("notexampleowner");
		DannoAccessPolicy accessPolicy = getAustlitAccessPolicy(ip);

		accessPolicy.checkRead(request, privateAnnoteaObject);
		assertEquals(0, privateAnnoteaObject.getRDFObjectURIs().size());

	}
	
	public void testCheckMultipleAllowed() throws Exception {
		DannoIdentityProvider ip = new MockDannoIdentityProvider("exampleowner");
		DannoAccessPolicy accessPolicy = getAustlitAccessPolicy(ip);
		
		accessPolicy.checkRead(request, multiAnnoContainer);
		assertEquals(2, multiAnnoContainer.getRDFObjectURIs().size());
	}
	
	public void testCheckMultipleNotAllowed() throws Exception {
		DannoIdentityProvider ip = new MockDannoIdentityProvider("notexampleowner");
		DannoAccessPolicy accessPolicy = getAustlitAccessPolicy(ip);
		
		accessPolicy.checkRead(request, multiAnnoContainer);
		assertEquals(1, multiAnnoContainer.getRDFObjectURIs().size());
	}
	
	public void testUnmarkedAnnotation() throws Exception {
		DannoIdentityProvider ip = new MockDannoIdentityProvider("notexampleowner");
		DannoAccessPolicy accessPolicy = getAustlitAccessPolicy(ip);
		
		accessPolicy.checkRead(request, unmarkedPublicAnnotation);
	}
	
	public void testHttpMessageRequest() throws Exception {
		DannoIdentityProvider ip = new MockDannoIdentityProvider("exampleowner");
		DannoAccessPolicy accessPolicy = getAustlitAccessPolicy(ip);
		
		accessPolicy.checkRead(request, httpMessage);
	}
	
	private DannoAccessPolicy getAustlitAccessPolicy(DannoIdentityProvider ip) {
		AustlitDannoAccessPolicy accessPolicy = new AustlitDannoAccessPolicy();
		accessPolicy.setIdentityProvider(ip);
		accessPolicy.setPrivateFieldName("http://auselit.metadata.net/privanno/private");
		accessPolicy.setPrivateValue("true");
		String[] authorities = {"ROLE_USER"};
		accessPolicy.setAuthenticationContext(new MockAuthenticationContext(
				"name", "id", authorities));
		return accessPolicy;
	}
	
	private RDFObject getSampleRDFObject(String anno) throws Exception {
		JenaAnnoteaTypeFactory factory = new JenaAnnoteaTypeFactory();

		InputStream in = new ByteArrayInputStream(anno.getBytes());
		return factory.createRDFObject(in, null);

	}

	/**
	 * A private annotation, owned by 'exampleowner'
	 * @return
	 */
	private String getPrivateAnnoString() {
		return "<r:RDF\n"
				+ "    xmlns:j.0=\"http://www.w3.org/2000/10/annotationType#\"\n"
				+ "    xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
				+ "    xmlns:d=\"http://purl.org/dc/elements/1.1/\"\n"
				+ "    xmlns:x=\"http://auselit.metadata.net/privanno/\"\n"
				+ "    xmlns:dn=\"http://metadata.net/2009/09/danno#\"\n"
				+ "    xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" > \n"
				+ "  <r:Description r:about=\"http://foo/bar\">\n"
				+ "    <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\"/>\n"
				+ "    <r:type r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\"/>\n"
				+ "    <a:annotates r:resource=\"http://serv1.example.com/some/page.html\"/>\n"
				+ "    <a:context>http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])</a:context>\n"
				+ "    <d:title>Annotation of Sample Page</d:title>\n"
				+ "    <d:creator>Ralph Swick</d:creator>\n"
				+ "    <a:created>1999-10-14T12:10:01Z</a:created>\n"
				+ "    <x:private>true</x:private>\n"
				+ "    <dn:owner>exampleowner</dn:owner>\n"
				+ "    <d:date>1999-10-14T12:10:01Z</d:date>\n"
				+ "    <a:body r:resource=\"http://serv2.example.com/mycomment.html\"/>\n"
				+ "  </r:Description>\n" + "</r:RDF>\n";
	}

	/**
	 * A public annotation, owned by 'exampleowner'
	 * @return
	 */
	private String getPublicAnnoString() {
		return "<r:RDF\n"
				+ "    xmlns:j.0=\"http://www.w3.org/2000/10/annotationType#\"\n"
				+ "    xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
				+ "    xmlns:d=\"http://purl.org/dc/elements/1.1/\"\n"
				+ "    xmlns:x=\"http://auselit.metadata.net/privanno/\"\n"
				+ "    xmlns:dn=\"http://metadata.net/2009/09/danno#\"\n"
				+ "    xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" > \n"
				+ "  <r:Description r:about=\"http://foo/scrap\">\n"
				+ "    <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\"/>\n"
				+ "    <r:type r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\"/>\n"
				+ "    <a:annotates r:resource=\"http://serv1.example.com/some/page.html\"/>\n"
				+ "    <a:context>http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])</a:context>\n"
				+ "    <d:title>Annotation of Sample Page</d:title>\n"
				+ "    <d:creator>Ralph Swick</d:creator>\n"
				+ "    <a:created>1999-10-14T12:10:01Z</a:created>\n"
				+ "    <x:private>false</x:private>\n"
				+ "    <dn:owner>exampleowner</dn:owner>\n"
				+ "    <d:date>1999-10-14T12:10:01Z</d:date>\n"
				+ "    <a:body r:resource=\"http://serv2.example.com/mycomment.html\"/>\n"
				+ "  </r:Description>\n" + "</r:RDF>\n";
	}

	/**
	 * A public annotation, without a privacy tag, owned by 'exampleowner'
	 * @return
	 */
	private String getUnmarkedPublicAnnoString() {
		return "<r:RDF\n"
				+ "    xmlns:j.0=\"http://www.w3.org/2000/10/annotationType#\"\n"
				+ "    xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
				+ "    xmlns:d=\"http://purl.org/dc/elements/1.1/\"\n"
				+ "    xmlns:x=\"http://auselit.metadata.net/privanno/\"\n"
				+ "    xmlns:dn=\"http://metadata.net/2009/09/danno#\"\n"
				+ "    xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" > \n"
				+ "  <r:Description r:about=\"http://foo/scrap\">\n"
				+ "    <r:type r:resource=\"http://www.w3.org/2000/10/annotation-ns#Annotation\"/>\n"
				+ "    <r:type r:resource=\"http://www.w3.org/2000/10/annotationType#Comment\"/>\n"
				+ "    <a:annotates r:resource=\"http://serv1.example.com/some/page.html\"/>\n"
				+ "    <a:context>http://serv1.example.com/some/page.html#xpointer(id(\"Main\")/p[2])</a:context>\n"
				+ "    <d:title>Annotation of Sample Page</d:title>\n"
				+ "    <d:creator>Ralph Swick</d:creator>\n"
				+ "    <a:created>1999-10-14T12:10:01Z</a:created>\n"
				+ "    <dn:owner>exampleowner</dn:owner>\n"
				+ "    <d:date>1999-10-14T12:10:01Z</d:date>\n"
				+ "    <a:body r:resource=\"http://serv2.example.com/mycomment.html\"/>\n"
				+ "  </r:Description>\n" + "</r:RDF>\n";
	}
	
	private String getHttpMessageString() {
		return "<rdf:RDF\r\n" + 
				"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" + 
				"    xmlns:j.0=\"http://www.w3.org/1999/xx/http#\" > \n" + 
				"  <rdf:Description rdf:about=\"http://localhost:8080/danno/annotea/body/00D6C6307BC5D038\">\n" + 
				"    <j.0:ContentType>text/html</j.0:ContentType>\n" + 
				"    <j.0:Body>&lt;html xmlns=\"http://www.w3.org/TR/REC-html40\"&gt;&lt;head&gt;&lt;title&gt;NZ lesbian detective series&lt;/title&gt;&lt;/head&gt;&lt;body&gt;&lt;H1 class=\"parseasinTitle\"&gt;&lt;SPAN id=\"btAsinTitle\"&gt;Introducing Amanda Valentine (Beecham, Rose, Amanda Valentine Mystery, 1.) (Paperback)&lt;/SPAN&gt;&lt;/H1&gt;&lt;/body&gt;&lt;/html&gt;</j.0:Body>\n" + 
				"  </rdf:Description>\n" + 
				"</rdf:RDF>\n";
	}
}
