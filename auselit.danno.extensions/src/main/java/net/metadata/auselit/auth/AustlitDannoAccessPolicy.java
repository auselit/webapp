package net.metadata.auselit.auth;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

import au.edu.diasb.annotation.danno.DannoAccessPolicy;
import au.edu.diasb.annotation.danno.DannoIdentityProvider;
import au.edu.diasb.annotation.danno.DannotateAccessPolicy;
import au.edu.diasb.annotation.danno.DefaultDannoIdentityProvider;
import au.edu.diasb.annotation.danno.model.AnnoteaObject;
import au.edu.diasb.annotation.danno.model.AnnoteaTypeException;
import au.edu.diasb.annotation.danno.model.RDFContainer;
import au.edu.diasb.chico.mvc.AuthenticationContext;
import au.edu.diasb.chico.mvc.DefaultAuthenticationContext;
import au.edu.diasb.chico.mvc.RequestFailureException;

public class AustlitDannoAccessPolicy implements DannoAccessPolicy, DannotateAccessPolicy,
		InitializingBean {

	private String privateValue;
	private String privateFieldName;

    private String[] adminAuthorities = {"ROLE_ADMIN"};
    private String[] readAuthorities = {"ROLE_USER"};
    private String[] writeAuthorities = {"ROLE_ANNOTATOR"};
    private String[] oaiAuthorities = {"ROLE_OAI"};
    private String[] useAuthorities = {"ROLE_USER"};

	private AuthenticationContext ac;
	private DannoIdentityProvider ip;

	private boolean webAdmin = false;
	private boolean checkOwner = true;
    private boolean userSettableName = false;

    public AustlitDannoAccessPolicy() {
    	super();
    }
    
    public AustlitDannoAccessPolicy(AuthenticationContext ac) {
    	super();
    	this.ac = ac;
    }
    
	public void afterPropertiesSet() {
		if (ac == null) {
			ac = new DefaultAuthenticationContext();
		}
		if (ip == null) {
			ip = new DefaultDannoIdentityProvider(ac);
		}
	}
    //
    // Dannotate access checks
    //
    
    /**
     * The requestor can change names if this is allowed globally or
     * if admin authority is granted.
     */
    public final boolean canChangeNames(HttpServletRequest request) {
        return userSettableName || ac.hasAuthority(request, adminAuthorities);
    }

    @Override
    public void checkUse(HttpServletRequest request) 
    throws RequestFailureException {
        ac.checkAuthority(request, useAuthorities);
    }

    @Override
    public void checkCreateOrEditAnnotations(HttpServletRequest request)
    throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
    }

    @Override
    public void checkCreateAnnotation(HttpServletRequest request, String schemaName) 
    throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
    }

    @Override
    public void checkEditAnnotation(HttpServletRequest request, String schemaName) 
    throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
    }

    @Override
    public void checkDelete(HttpServletRequest request)
    throws RequestFailureException{
        ac.checkAuthority(request, writeAuthorities);
    }
    
    //
    // Danno access checks
    //
    
    @Override
    public void checkWebAdmin(HttpServletRequest request)
			throws RequestFailureException {
		if (!webAdmin) {
			throw new RequestFailureException(
					HttpServletResponse.SC_BAD_REQUEST,
					"Danno admin requests are disabled");
		}
		ac.checkAuthority(request, adminAuthorities);
	}

	public void checkCreate(HttpServletRequest request, RDFContainer res)
			throws RequestFailureException {
		ac.checkAuthority(request, writeAuthorities);
	}

	public void checkDelete(HttpServletRequest request, AnnoteaObject obj)
			throws RequestFailureException {
		if (ac.hasAuthority(null, adminAuthorities)) {
			return;
		}
		ac.checkAuthority(request, writeAuthorities);
		checkObjectOwner(request, obj);
	}

	/**
	 * Checks whether user has permission to read the object(s)
	 * 
	 * If an annotation is marked Private, it must be owned by the requesting
	 * user to be viewed.
	 * 
	 * Checks all of the Annotea objects in the container, removing any that
	 * are marked as private and don't belong to the current user.
	 */
	public void checkRead(HttpServletRequest request, RDFContainer res)
			throws RequestFailureException {
		ac.checkAuthority(request, readAuthorities);

		Set<String> rdfObjectURIs = res.getRDFObjectURIs();
		
		for (String uri : rdfObjectURIs) {
			removeIfPrivate(request, res, uri);
		}
	}

	private void removeIfPrivate(HttpServletRequest request,
			RDFContainer resource, String uri) {
		RDFContainer rdfContainer = resource.extractResourceClosure(uri, 0);
		try {		
			AnnoteaObject annotation = rdfContainer.getAnnoteaObject();
	
			if (isPrivateAnnotation(annotation)) {
				String ownerId = annotation.getOwnerId();
				if (ownerId == null) {
					return;
				}
				String userUri = ip.obtainUserURI(request);
				if (userUri == null || !ownerId.equals(userUri)) {
					resource.removeResource(uri);
				}
			}
		} catch (AnnoteaTypeException e) {
			// Not an Annotea object, so reading it is fine.
		}
	}

	public boolean isPrivateAnnotation(AnnoteaObject obj) {
		String prop = obj.getLiteralProperty(privateFieldName);

		if (prop == null) {
			return false;
		}
		return prop.equals(privateValue);
	}

	public void checkUpdate(HttpServletRequest request, AnnoteaObject obj)
			throws RequestFailureException {
		ac.checkAuthority(request, writeAuthorities);
		checkObjectOwner(request, obj);
	}

	private void checkObjectOwner(HttpServletRequest request,
			AnnoteaObject obj) {
		if (!checkOwner) {
			return;
		}
		String ownerId = obj.getOwnerId();
		if (ownerId == null) {
			return;
		}
		String userUri = ip.obtainUserURI(request);
		if (userUri == null) {
			throw new AccessDeniedException(
					"Ooops ... cannot establish your identity");
		} else if (!ownerId.equals(userUri)) {
			// throw new RequestFailureException(
			// HttpServletResponse.SC_FORBIDDEN,
			// "Permission denied reading annotation");
			throw new AccessDeniedException("You do not own this object");
		}
	}

    //
    // Getters and setters
    //

	public String[] getAdminAuthorityList() {
		return adminAuthorities;
	}

	/**
	 * Set the name of the admin authority; e.g. "ROLE_ADMIN".
	 * @param adminAuthority
	 */
	public void setAdminAuthorities(String adminAuthorities) {
        this.adminAuthorities = 
            StringUtils.commaDelimitedListToStringArray(adminAuthorities);
	}

	public String[] getReadAuthorityList() {
		return readAuthorities;
	}

	/**
	 * Set the names of the read authorities; e.g. "ROLE_USER".
	 * @param readAuthorities
	 */
	public void setReadAuthorities(String readAuthorities) {
        this.readAuthorities = 
            StringUtils.commaDelimitedListToStringArray(readAuthorities);
	}

	public String[] getWriteAuthorityList() {
		return writeAuthorities;
	}

	/**
	 * Set the names of the write authorities; e.g. "ROLE_ANNOTATOR".
	 * @param writeAuthorities
	 */
	public void setWriteAuthorities(String writeAuthorities) {
        this.writeAuthorities = 
            StringUtils.commaDelimitedListToStringArray(writeAuthorities);
	}

	public String[] getOaiAuthorityList() {
		return oaiAuthorities;
	}

	/**
	 * Set the names of the OAI authorities; e.g. "ROLE_OAI".
	 * @param oaiAuthorities
	 */
	public void setOaiAuthorities(String oaiAuthorities) {
        this.oaiAuthorities = 
            StringUtils.commaDelimitedListToStringArray(oaiAuthorities);
	}

	public AuthenticationContext getAuthenticationContext() {
		return ac;
	}

	public void setAuthenticationContext(AuthenticationContext ac) {
		this.ac = ac;
	}

	public DannoIdentityProvider getIdentityProvider() {
		return ip;
	}

	public void setIdentityProvider(DannoIdentityProvider ip) {
		this.ip = ip;
	}

	public boolean isWebAdmin() {
		return webAdmin;
	}

	/**
     * This property enables Danno's web admin requests. 
     * It defaults to {@literal false}.
	 * 
	 * @param webAdmin
	 */
	public void setWebAdmin(boolean webAdmin) {
		this.webAdmin = webAdmin;
	}

	public boolean isCheckOwner() {
		return checkOwner;
	}

	/**
     * This property determines if we should check the ownership of
     * the target annotation before editing or deleting it.  It defaults
     * to {@literal true}.
	 * 
     * @param checkOwner 
	 */
	public void setCheckOwner(boolean checkOwner) {
		this.checkOwner = checkOwner;
	}

    public boolean isUserSettableName() {
        return userSettableName;
    }
    
    /**
     * This property determines if we allow the user to set a user
     * name that is different to what his / her credentials say. 
     * It defaults to {@literal false}.
     * 
     * @param userSettableName 
     */
    public void setUserSettableName(boolean userSettableName) {
        this.userSettableName = userSettableName;
    }

    public String[] getUseAuthorityList() {
        return useAuthorities;
    }

    public void setUseAuthorities(String useAuthorities) {
        this.useAuthorities = 
            StringUtils.commaDelimitedListToStringArray(useAuthorities);
    }
    
	/**
	 * @return the privateValue
	 */
	public String getPrivateValue() {
		return privateValue;
	}

	/**
	 * @param privateValue
	 *            the privateValue to set
	 */
	public void setPrivateValue(String privateValue) {
		this.privateValue = privateValue;
	}

	/**
	 * @return the privateFieldName
	 */
	public String getPrivateFieldName() {
		return privateFieldName;
	}

	/**
	 * @param privateFieldName
	 *            the privateFieldName to set
	 */
	public void setPrivateFieldName(String privateFieldName) {
		this.privateFieldName = privateFieldName;
	}

    @Override
    public String toString() {
        return "DefaultAccessPolicy{webAdmin=" + webAdmin +
                ",userSettableName=" + userSettableName +
                ",checkOwner=" + checkOwner +
                ",useAuthorities=" + 
                StringUtils.arrayToCommaDelimitedString(useAuthorities) +
                ",oaiAuthorities=" + 
                StringUtils.arrayToCommaDelimitedString(oaiAuthorities) +
                ",readAuthorities=" + 
                StringUtils.arrayToCommaDelimitedString(readAuthorities) +
                ",writeAuthorities=" + 
                StringUtils.arrayToCommaDelimitedString(writeAuthorities) +
                ",adminAuthorities=" + 
                StringUtils.arrayToCommaDelimitedString(adminAuthorities) + "}";
    }

}
