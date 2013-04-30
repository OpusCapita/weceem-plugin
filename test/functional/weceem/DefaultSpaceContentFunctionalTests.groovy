package weceem

import com.grailsrocks.functionaltest.*

class DefaultSpaceContentFunctionalTests extends BrowserTestCase {
	void testHomePage() {
		javaScriptEnabled = false

		get('/')

		expect status:200, contentContains: 'welcome'
	}

	void testSearch() {
		javaScriptEnabled = false

		get('/')

		expect status:200, contentContains: 'welcome'

		form {
			query = 'about'
			click "Search"
		}

		expect status:200
		expect contentContains: 'about us'
	}	
}

