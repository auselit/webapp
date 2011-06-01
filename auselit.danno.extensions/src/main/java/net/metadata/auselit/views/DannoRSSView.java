package net.metadata.auselit.views;

import static au.edu.diasb.danno.constants.AnnoteaProtocolConstants.DANNO_USE_STYLESHEET;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.feed.AbstractRssFeedView;

import au.edu.diasb.annotation.danno.DannoResponse;
import au.edu.diasb.annotation.danno.common.DannoProperties;
import au.edu.diasb.annotation.danno.model.AnnoteaObject;
import au.edu.diasb.annotation.danno.model.RDFContainer;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;

/**
 * This view renders an RSS response containing RDF annotations/replies
 * 
 * Uses the ROME library.
 * 
 * @author Damien Ayers
 */
public class DannoRSSView extends AbstractRssFeedView {

    private Properties properties;
	@Override
	protected void buildFeedMetadata(Map<String, Object> model, Channel feed,
			HttpServletRequest request) {

        String baseURI = getProperties().getProperty(DannoProperties.HOME_URL_PROP);
		feed.setLink(baseURI);

        String feedTitle = (String) model.get(DannoResponse.RSS_FEED_TITLE_KEY);
        feed.setDescription(feedTitle);
        feed.setTitle("Danno RSS feed");
        
        feed.setLanguage("en");
//        feed.setWebMaster("joe.bloe@itee.uq.edu.au");
	}
	
	@Override
	protected List<Item> buildFeedItems(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		List<Item> items = new ArrayList<Item>();

        RDFContainer responseRDF = (RDFContainer) model.get(DannoResponse.RESPONSE_RDF_KEY);
        
        for (String uri : responseRDF.getRDFObjectURIs()) {
            AnnoteaObject object = responseRDF.getRDFObject(uri, true, false).getAnnoteaObject();
            if (object != null) {
            	Item item = new Item();
            	item.setTitle(object.getTitle());
            	item.setLink(uri + "?" + DANNO_USE_STYLESHEET);
            	item.setAuthor("nospam@example.com + (" + object.getCreator() + ")");
            	item.setPubDate(object.getDate().toGregorianCalendar().getTime());
            	items.add(item);
            }
        }
		return items;
	}

    
    public void setProperties(Properties props) {
        this.properties = props;
    }

    public Properties getProperties() {
        return properties;
    }
}
