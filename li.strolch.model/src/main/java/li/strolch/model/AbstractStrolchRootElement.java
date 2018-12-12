package li.strolch.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.model.xml.StrolchElementToXmlStringVisitor;

public abstract class AbstractStrolchRootElement extends GroupedParameterizedElement implements StrolchRootElement {

	public AbstractStrolchRootElement() {
		super();
	}

	public AbstractStrolchRootElement(String id, String name, String type) {
		super(id, name, type);
	}

	@Override
	public String toXmlString() {
		return accept(new StrolchElementToXmlStringVisitor().withoutDocument());
	}

	@Override
	public String toJsonString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.accept(new StrolchRootElementToJsonVisitor()));
	}

	@Override
	public String toFlatJsonString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.accept(new StrolchRootElementToJsonVisitor().flat()));
	}
}
