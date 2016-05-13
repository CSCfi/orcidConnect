package fi.csc.orcidconnect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import fi.csc.orcidconnect.push.soap.schema.identitiesdescriptor.Identifier;
import fi.csc.orcidconnect.push.soap.schema.identitiesdescriptor.IdentityDescriptor;

@Component
@ConfigurationProperties(prefix="my")
public class IdentitiesRelayerConfiguration {

	final private String propertyPrefix = "my.pushInterfaceSpecs.";
	
	List<String> pushInterfaces;
	
	@Autowired
	private Environment env;

	public List<String> getPushInterfaces() {
		return pushInterfaces;
	}

	public void setPushInterfaces(List<String> pushInterfaces) {
		this.pushInterfaces = pushInterfaces;
	}
	
	@SuppressWarnings("rawtypes")
	public IdentitiesRelayer implPicker(IdentityDescriptor idDescr) {
		String className = implNamePicker(idDescr);
		try {
			IdentitiesRelayer i;
			Constructor con;
			switch (className) {
			case "fi.csc.orcidconnect.push.soap.MockSoapClient":
				con = Class.forName(className).getConstructor();
				i = (IdentitiesRelayer) con.newInstance();
				break;
			case "fi.csc.orcidconnect.push.rest.RestJsonClient":
				con = Class.forName(className).getConstructor(String.class);
				i = (IdentitiesRelayer) con
						.newInstance(
								getRestUrl(idDescr)
								);
				break;
			default:
				i = null;
				break;
			}
			
			
			return i;
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	private String implNamePicker(IdentityDescriptor idDescr) {
		return implNamePicker(
				getOrg(idDescr)
				);
	}
	
	private String implNamePicker(String org) {
		return env.getProperty(
				propertyPrefix + org + ".relayerImplClass");
	}
	
	private String getRestUrl(IdentityDescriptor id) {
		return env.getProperty(propertyPrefix +
				getOrg(id)
				+ ".restUrl"
				);
	}
	
	private String getOrg(IdentityDescriptor id) {
		return findOrgByIdP(
				getEppnIssuer(id));
	}
	
	private static String getEppnIssuer (IdentityDescriptor idDescr) {
		Identifier id =
				idDescr.findFirstIdentifierWithFn(Identifier.eppnFrName);
		return id.getIssuer();
	}
	
	private String findOrgByIdP (String entityId) {
		if (isNotInit()) throw new IllegalStateException();
		for (Iterator<String> i = pushInterfaces.iterator(); i.hasNext(); ) {
			String org = i.next();
			String cIdp = env.getProperty("my.pushInterfaceSpecs." + org + ".idp");
			if (cIdp.equals(entityId)) return org; 
		}
		return "";
	}
	
	private boolean isNotInit() {
		return pushInterfaces == null;
	}
	
}