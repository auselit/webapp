package net.metadata.auselit.indexing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.metadata.auselit.auth.AustlitDannoAccessPolicy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.common.util.ContentStreamBase.StringStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import au.edu.diasb.annotation.danno.DannoControllerConfig;
import au.edu.diasb.annotation.danno.common.DannoProperties;
import au.edu.diasb.annotation.danno.db.RDFDBContainer;
import au.edu.diasb.annotation.danno.model.Annotation;
import au.edu.diasb.annotation.danno.model.AnnoteaObject;
import au.edu.diasb.annotation.danno.model.HTTPMessage;
import au.edu.diasb.annotation.danno.model.RDFContainer;
import au.edu.diasb.annotation.danno.sparql.SPARQLResult;
import au.edu.diasb.annotation.danno.sparql.SPARQLResultSet;
import au.edu.diasb.annotation.danno.update.SecondaryUpdateException;
import au.edu.diasb.annotation.danno.update.SecondaryUpdateHandler;

public class SolrSecondaryUpdateHandler implements SecondaryUpdateHandler,
		InitializingBean {
	private String baseURI;
	private DannoProperties props;
	private String solrUrl;
	private String solrHostname;
	private int solrPort;
	private String solrRealm;
	private String solrUsername;
	private String solrPassword;
	private boolean clearIndexOnReset;
	private SolrServer server;
	private AustlitDannoAccessPolicy adap;
	private DannoControllerConfig dcc;

    private final Logger logger = Logger.getLogger(this.getClass());
    
	public void setSolrUrl(String solrUrl) {
		this.solrUrl = solrUrl;
	}

	/**
	 * Used to determine whether an object is "private". Private objects are not
	 * stored in the Solr index.
	 * 
	 * @param adap
	 */
	public void setDannoAccessPolicy(AustlitDannoAccessPolicy adap) {
		this.adap = adap;
	}

	/**
	 * Set the common properties object. This property is mandatory.
	 * 
	 * @param format
	 */
	public final void setProps(Properties props) {
		this.props = DannoProperties.asInstance(props);
	}

	public void setClearIndexOnReset(boolean clearIndexOnReset) {
		this.clearIndexOnReset = clearIndexOnReset;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(solrUrl, "'solrUrl' property not set");
		Assert.notNull(props, "'props' property not set");

		setupServer();

		baseURI = props.getNonemptyProperty(DannoProperties.ANNOTEA_URL_PROP);
	}

	private void setupServer() throws MalformedURLException {
		CommonsHttpSolrServer server;
		if (solrUsername != null && !solrUsername.isEmpty()) {
			HttpClient httpClient = new HttpClient();
			httpClient.getState()
					.setCredentials(
							new AuthScope(solrHostname, solrPort, solrRealm),
							new UsernamePasswordCredentials(solrUsername,
									solrPassword));
			httpClient.getParams().setAuthenticationPreemptive(true);

			server = new CommonsHttpSolrServer(solrUrl, httpClient);
		} else {
			server = new CommonsHttpSolrServer(solrUrl);
		}
		this.server = server;
	}

	@Override
	public void refreshEnd() {
	}

	@Override
	public void refreshStart() {
		reset();
	}

	@Override
	public void reset() {
		try {
			if (clearIndexOnReset)
				server.deleteByQuery("*:*");
		} catch (SolrServerException ex) {
			throw new SecondaryUpdateException("Error emptying Solr Index", ex);
		} catch (IOException ex) {
			throw new SecondaryUpdateException("Error emptying Solr Index", ex);
		}

	}

	/**
	 * Attempts to update the solr index by adding and removing the changed objects.
	 * If the index cannot be updated, it will still return successfully, since Danno
	 * has already saved the object, and the user should be notified of that, this index
	 * is not essential.
	 */
	@Override
	public void update(List<RDFContainer> savedObjects,
			List<String> deletedObjectURIs) {

		List<String> deletedIds = new ArrayList<String>();
		for (String uri : deletedObjectURIs) {
			// deleted objects are both bodies and annotations, seems to be okay
			deletedIds.add(uri);
		}

		try {
			Collection<SolrInputDocument> docsToSend = sendDocsToSolr(savedObjects);
			if (docsToSend.size() > 0) {
				server.add(docsToSend);
			}
			if (deletedIds.size() > 0) {
				server.deleteById(deletedIds);
			}
			server.commit();
		} catch (SolrServerException ex) {
			logger.error("Exception updating Solr index", ex);
		} catch (IOException ex) {
			logger.error("Exception updating Solr index", ex);
		} catch (Exception ex) {
			logger.error("Exception updating Solr index", ex);
		}
	}

	private Collection<SolrInputDocument> sendDocsToSolr(
			List<RDFContainer> objects) throws SolrServerException, IOException {
		Collection<SolrInputDocument> updateDocs = new ArrayList<SolrInputDocument>();
		Map<String, RDFContainer> bodies = new HashMap<String, RDFContainer>();
		List<AnnoteaObject> annotations = new ArrayList<AnnoteaObject>();
		
		// Split savedObjects into annos and bodies
		for (RDFContainer object : objects) {
			String uri = rdfObjectToUri(object);

			// FIXME: Make this right, instead of hacky
			if (uri.contains("body")) {
				bodies.put(uri, object);
			} else {
				AnnoteaObject annoteaObject = object.getAnnoteaObject();

				if (!adap.isPrivateAnnotation(annoteaObject)) {
					annotations.add(annoteaObject);
				}
			}
		}
		
		if (bodies.size() == 0) {
			RDFDBContainer container = dcc.getContainerFactory().connect(false);
			try {
				for (AnnoteaObject ao : annotations) {
					Set<String> bodyUris = ao.getBodyUris();
					for (String uri : bodyUris) {
						bodies.put(uri,container.getRDFObject(uri, true, false));
					}
				}
			} finally {
				container.close();
			}
			
		}
		

		for (AnnoteaObject ao : annotations) {
			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("type", "annotation");
			doc.addField("title", ao.getTitle());
			doc.addField("created", ao.getCreated().toGregorianCalendar()
					.getTime());
			doc.addField("creator", ao.getCreator());
			if (ao.getModified() != null)
				doc.addField("last_modified", ao.getModified().toGregorianCalendar()
						.getTime());
			doc.addField("id", rdfObjectToUri(ao));

			Set<String> bodyUris = ao.getBodyUris();
			for (String bodyUri : bodyUris) {
				RDFContainer rdfContainer = bodies.get(bodyUri);
				if (rdfContainer != null) {
					HTTPMessage httpMessage = rdfContainer.getHTTPMessage();
					doc.addField("body_stripped", httpMessage.getBody());
				}
			}

			if (ao instanceof Annotation) {
				Annotation anno = (Annotation) ao;
				for (String annotates : anno.getAnnotates()) {
					doc.addField("annotates", annotates);
				}
			}

			//FIXME: Ideally tags should be extracted from annotations, but it needs a bit more code
			// tags can either be a URI reference to austlit, or a plain text string.
//			String vanno = "http://austlit.edu.au/ontologies/2009/03/lit-annotation-ns#";
//			String tagURI = vanno + "tag";
//			SPARQLResultSet executeSelect = ao
//					.getQueryFactory()
//					.createTupleQuery(
//							"SELECT ?tag WHERE {?anno <" + tagURI
//									+ "> ?tag2 .  FILTER (str(?tag) = ?tag2)  }").executeSelect(ao);
//			for (SPARQLResult sr : executeSelect) {
////				RDFObject object = sr.getObject("?tag");
//				doc.addField("tag", sr.getLiteral("?tag"));
////				sr.getResource("b").getURI();
//			}

			updateDocs.add(doc);

		}
		return updateDocs;
	}

	private static String rdfObjectToUri(RDFContainer object) {
		Set<String> uris = object.getRDFObjectURIs();
		if (uris.size() != 1) {
			throw new SecondaryUpdateException("wrong number of object URIs: "
					+ uris.size());
		}
		return uris.iterator().next();
	}

	private String uriToId(String uri) {
		if (!uri.startsWith(baseURI)) {
			throw new SecondaryUpdateException("URI (" + uri
					+ ") does not start with baseURI (" + baseURI + ")");
		}
		return uri.substring(baseURI.length());
	}

	
	/**
	 * Unused code for posting to a Solr extractor, designed to process HTML
	 * and other even weightier file formats. Turns out this isn't required 
	 * to just strip HTML from a field.
	 * @param bodies
	 * @param ao
	 */
	private void uploadToDocumentExtractor(Map<String, RDFContainer> bodies,
			AnnoteaObject ao) {
		ContentStreamUpdateRequest up = new ContentStreamUpdateRequest(
				"/update/extract");
		Set<String> bodyUris = ao.getBodyUris();
		String body = "";
		for (String bodyUri : bodyUris) {
			RDFContainer rdfContainer = bodies.get(bodyUri);
			HTTPMessage httpMessage = rdfContainer.getHTTPMessage();
			body += httpMessage.getBody();
			// doc.addField("body", httpMessage.getBody());
		}

		StringStream stringStream = new ContentStreamBase.StringStream(body);
		up.addContentStream(stringStream);
		String LITERALS_PREFIX = "literal.";
		up.setParam(LITERALS_PREFIX + "id", uriToId(rdfObjectToUri(ao)));
		up.setParam(LITERALS_PREFIX + "type", "annotation");
		up.setParam(LITERALS_PREFIX + "title", ao.getTitle());
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss zzz");
		up.setParam(LITERALS_PREFIX + "created",
				sdf.format(ao.getCreated().toGregorianCalendar().getTime()));
		if (ao.getModified() != null)
			up.setParam(LITERALS_PREFIX + "modified", sdf.format(ao
					.getModified().toGregorianCalendar().getTime()));
		up.setParam(LITERALS_PREFIX + "creator", ao.getCreator());

		// causes all generated fields that aren't defined in the schema
		// to be prefixed with attr_ (which is a dynamic field that is stored)
		up.setParam("uprefix", "attr_");

		// Map the content to the body field
		up.setParam("fmap.content", "body");

		// up.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);

		try {
			server.request(up);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void setSolrHostname(String solrHostname) {
		this.solrHostname = solrHostname;
	}

	public void setSolrPort(int solrPort) {
		this.solrPort = solrPort;
	}

	public void setSolrRealm(String solrRealm) {
		this.solrRealm = solrRealm;
	}

	public void setSolrUsername(String solrUsername) {
		this.solrUsername = solrUsername;
	}

	public void setSolrPassword(String solrPassword) {
		this.solrPassword = solrPassword;
	}

	public DannoControllerConfig getDannoControllerConfig() {
		return dcc;
	}

	public void setDannoControllerConfig(DannoControllerConfig dannoControllerConfig) {
		this.dcc = dannoControllerConfig;
	}
}
