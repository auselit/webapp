package net.metadata.auselit.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

import org.json.JSONArray;

import au.edu.diasb.annotation.danno.model.AnnoteaObject;
import au.edu.diasb.annotation.danno.model.AnnoteaTypeException;
import au.edu.diasb.annotation.danno.model.HTTPMessage;
import au.edu.diasb.annotation.danno.model.RDFContainer;
import au.edu.diasb.annotation.danno.model.RDFLiteral;
import au.edu.diasb.annotation.danno.model.RDFObject;
import au.edu.diasb.annotation.danno.model.RDFParserException;
import au.edu.diasb.annotation.danno.model.RDFResource;
import au.edu.diasb.annotation.danno.model.RDFStatement;
import au.edu.diasb.annotation.danno.model.RDFTooComplexException;
import au.edu.diasb.annotation.danno.sparql.SPARQLQueryFactory;

public class MockRDFContainer implements RDFContainer {

	public void add(RDFStatement statement) {
		// TODO Auto-generated method stub

	}

	public void add(RDFResource resource) {
		// TODO Auto-generated method stub

	}

	public void addStatement(String subject, String predicate, String object) {
		// TODO Auto-generated method stub

	}

	public void addAll(Collection<RDFResource> resources) {
		// TODO Auto-generated method stub

	}

	public void addAll(RDFResource[] resources) {
		// TODO Auto-generated method stub

	}

	public void addAll(RDFContainer container) {
		// TODO Auto-generated method stub

	}

	public void addAll(InputStream in) throws IOException, RDFParserException {
		// TODO Auto-generated method stub

	}

	public RDFObject getRDFObject(String uriOrId, boolean extract,
			boolean isBlankNode) throws AnnoteaTypeException {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<String> getRDFObjectURIs() {
		// TODO Auto-generated method stub
		return null;
	}

	public RDFContainer extractResourceClosure(String uri,
			int blankNodeClosureDepth) throws AnnoteaTypeException {
		// TODO Auto-generated method stub
		return null;
	}

	public AnnoteaObject getAnnoteaObject() throws AnnoteaTypeException {
		// TODO Auto-generated method stub
		return null;
	}

	public HTTPMessage getHTTPMessage() throws AnnoteaTypeException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean removeResource(String uri) {
		// TODO Auto-generated method stub
		return false;
	}

	public RDFLiteral createLiteral(String value) {
		// TODO Auto-generated method stub
		return null;
	}

	public void serialize(OutputStream stream, String format)
			throws RDFTooComplexException {
		// TODO Auto-generated method stub

	}

	public void serialize(Writer writer, String format)
			throws RDFTooComplexException {
		// TODO Auto-generated method stub

	}

	public Collection<RDFStatement> getAllStatements() {
		// TODO Auto-generated method stub
		return null;
	}

	public RDFContainer annoteaQuery(String predicate, String objectURI) {
		// TODO Auto-generated method stub
		return null;
	}

	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void dumpState(OutputStream dump) throws IOException {
		// TODO Auto-generated method stub

	}

	public void loadState(InputStream dump) throws IOException,
			RDFParserException {
		// TODO Auto-generated method stub

	}

	public SPARQLQueryFactory getQueryFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSlice() {
		// TODO Auto-generated method stub
		return false;
	}

	public JSONArray toJSON(boolean dannotateMode)
			throws RDFTooComplexException {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

}
