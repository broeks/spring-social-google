/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.google.api.impl;

import static java.util.Collections.singletonList;
import static org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.codehaus.jackson.map.SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS;
import static org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS;
import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;
import static org.springframework.http.MediaType.APPLICATION_ATOM_XML;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.drive.DriveOperations;
import org.springframework.social.google.api.drive.impl.DriveTemplate;
import org.springframework.social.google.api.legacyprofile.LegacyProfileOperations;
import org.springframework.social.google.api.legacyprofile.impl.UserTemplate;
import org.springframework.social.google.api.plus.activity.ActivityOperations;
import org.springframework.social.google.api.plus.activity.impl.ActivityTemplate;
import org.springframework.social.google.api.plus.comment.CommentOperations;
import org.springframework.social.google.api.plus.comment.impl.CommentTemplate;
import org.springframework.social.google.api.plus.person.PersonOperations;
import org.springframework.social.google.api.plus.person.impl.PersonTemplate;
import org.springframework.social.google.api.tasks.TaskOperations;
import org.springframework.social.google.api.tasks.impl.TaskTemplate;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;
import org.springframework.social.oauth2.OAuth2Version;

/**
 * <p>
 * The central class for interacting with Google APIs.
 * </p>
 * <p>
 * Most of the APIs, specifically all GData APIs and Google+ usage of "me", require OAuth2 authentication.
 * To use methods that require OAuth2 authentication, {@link GoogleTemplate} must be constructed with
 * an access token which is authorized for the appropriate scope.
 * For using Google+ without authenticating, {@link GoogleTemplate} default constructor can be used.
 * </p>
 * @author Gabriel Axel
 */
public class GoogleTemplate extends AbstractOAuth2ApiBinding implements Google {
	
	private String accessToken;

	private LegacyProfileOperations userOperations;
	private PersonOperations profileOperations;
	private ActivityOperations activityOperations;
	private CommentOperations commentOperations;
	private TaskOperations taskOperations;
	private DriveOperations driveOperations;
	
	/**
	 * Creates a new instance of GoogleTemplate.
	 * This constructor creates a new GoogleTemplate able to perform unauthenticated operations against Google+.
	 */
	public GoogleTemplate() {
		initialize();
	}
	
	/**
	 * Creates a new instance of GoogleTemplate.
	 * This constructor creates the FacebookTemplate using a given access token.
	 * @param accessToken an access token granted by Google after OAuth2 authentication
	 */
	public GoogleTemplate(String accessToken) {
		super(accessToken);
		this.accessToken = accessToken;
		initialize();
	}

	private void initialize() {
		userOperations = new UserTemplate(getRestTemplate(), isAuthorized());
		profileOperations = new PersonTemplate(getRestTemplate(), isAuthorized());
		activityOperations = new ActivityTemplate(getRestTemplate(), isAuthorized());
		commentOperations = new CommentTemplate(getRestTemplate(), isAuthorized());
		taskOperations = new TaskTemplate(getRestTemplate(), isAuthorized());
		driveOperations = new DriveTemplate(getRestTemplate(), isAuthorized());
	}
	
	@Override
	protected List<HttpMessageConverter<?>> getMessageConverters() {
		
		MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
		objectMapper.configure(FAIL_ON_EMPTY_BEANS, false);
		objectMapper.setSerializationInclusion(NON_NULL);
		jsonConverter.setObjectMapper(objectMapper);
		
		SourceHttpMessageConverter<Source> sourceConverter = new SourceHttpMessageConverter<Source>();
		sourceConverter.setSupportedMediaTypes(singletonList(APPLICATION_ATOM_XML));

		FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
		formHttpMessageConverter.addPartConverter(jsonConverter);
		
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(jsonConverter);
		messageConverters.add(sourceConverter);
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(formHttpMessageConverter);
		return messageConverters;
	}
	
	@Override
	protected OAuth2Version getOAuth2Version() {
		return OAuth2Version.BEARER;
	}

	@Override
	public LegacyProfileOperations userOperations() {
		return userOperations;
	}

	@Override
	public PersonOperations personOperations() {
		return profileOperations;
	}

	@Override
	public ActivityOperations activityOperations() {
		return activityOperations;
	}

	@Override
	public CommentOperations commentOperations() {
		return commentOperations;
	}

	@Override
	public TaskOperations taskOperations() {
		return taskOperations;
	}
	
	@Override
	public DriveOperations driveOperations() {
		return driveOperations;
	}
	
	@Override
	public void applyAuthentication(Object client) {
		Method setHeaders = findMethod(client.getClass(), "setHeader", String.class, String.class);
		invokeMethod(setHeaders, client, 
			"Authorization", getOAuth2Version().getAuthorizationHeaderValue(accessToken));
	}

	@Override
	public String getAccessToken() {
		return accessToken;
	}

}