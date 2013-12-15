/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.persistence.impl.model;

import javax.xml.parsers.DocumentBuilder;

import li.strolch.model.Resource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ch.eitchnet.xmlpers.api.DomParser;
import ch.eitchnet.xmlpers.util.DomUtil;

public class ResourceDomParser implements DomParser<Resource> {

	private Resource resource;

	@Override
	public Resource getObject() {
		return this.resource;
	}

	@Override
	public void setObject(Resource resource) {
		this.resource = resource;
	}

	@Override
	public Document toDom() {

		DocumentBuilder documentBuilder = DomUtil.createDocumentBuilder();
		Document document = documentBuilder.getDOMImplementation().createDocument(null, null, null);

		Element resourceDom = this.resource.toDom(document);
		document.appendChild(resourceDom);

		return document;
	}

	@Override
	public void fromDom(Document document) {

		Element rootElement = document.getDocumentElement();
		Resource resource = new Resource(rootElement);
		this.resource = resource;
	}
}