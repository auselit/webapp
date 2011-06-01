package net.metadata.auselit.auth;

import javax.servlet.http.HttpServletRequest;

import au.edu.diasb.annotation.danno.DannoIdentityProvider;

public class MockDannoIdentityProvider implements DannoIdentityProvider {

	private String userURI;
	
	
	public MockDannoIdentityProvider(String userURI) {
		super();
		this.userURI = userURI;
	}

	public String obtainUserURI(HttpServletRequest request) {
		return userURI;
	}
	
	public String obtainHumanReadableName(HttpServletRequest request,
			boolean ignoreNameInPreferenceCookie) {
		// TODO Auto-generated method stub
		return null;
	}

}
