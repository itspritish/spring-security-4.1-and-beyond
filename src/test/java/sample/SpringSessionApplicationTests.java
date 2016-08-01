/*
 * Copyright 2002-2016 the original author or authors.
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
package sample;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;

import sample.mvc.model.MessageDto;

/**
 *
 * @author Rob Winch
 * @author Joe Grandja
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringSessionApplication.class)
@AutoConfigureMockMvc
public class SpringSessionApplicationTests {

	@Autowired
	MockMvc mockMvc;


	@Test
	public void securityEnabled() throws Exception {
		mockMvc
				.perform(get("/"))
				.andExpect(status().isUnauthorized());
	}

	/*
		Angular Cross Site Request Forgery (XSRF) Protection
		https://docs.angularjs.org/api/ng/service/$http
	 */
	@Test
	public void deleteJoesMessage_SimulateAngular() throws Exception {
		MvcResult mvcResult = getJoesMessages();

		List<MessageDto> messages = JsonUtil.readValue(
				mvcResult.getResponse().getContentAsString(), new TypeReference<List<MessageDto>>(){});

		String csrfToken = extractCsrfToken(mvcResult.getResponse());

		mockMvc.perform(delete("/messages/{id}", messages.get(0).getId())
				.header("X-Requested-With", "XMLHttpRequest")
				.header("X-XSRF-TOKEN", csrfToken)
				.cookie(mvcResult.getResponse().getCookies()))
				.andExpect(status().isOk());
	}

	@Test
	public void accessHomeUnauthenticatedRedirectsToFormLogin() throws Exception {
		mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/custom-login"));
	}

	@Test
	public void logoutSuccess_SimulateAngular() throws Exception {
		MvcResult mvcResult = getJoesMessages();

		String csrfToken = extractCsrfToken(mvcResult.getResponse());

		mockMvc.perform(post("/logout")
				.accept(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
				.header("X-Requested-With", "XMLHttpRequest")
				.header("X-XSRF-TOKEN", csrfToken)
				.cookie(mvcResult.getResponse().getCookies()))
				.andExpect(status().isOk());
	}

	private MvcResult getJoesMessages() throws Exception {
		MvcResult mvcResult = mockMvc.perform(get("/messages/inbox")
				.with(httpBasic("joe@example.com", "password"))
				.header("X-Requested-With", "XMLHttpRequest"))
				.andExpect(status().isOk())
				.andReturn();

		return mvcResult;
	}

	private String extractCsrfToken(MockHttpServletResponse response) {
		String csrfToken = "";
		for (Cookie cookie : response.getCookies()) {
			if ("XSRF-TOKEN".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return csrfToken;
	}
}