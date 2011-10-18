dataSource {
	pooled = true
	driverClassName = "org.hsql.jdbcDriver"
	username = "sa"
	password = ""
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true // Warning, turning this on causes lock contention
    cache.provider_class='org.hibernate.cache.EhCacheProvider'
}
// environment specific settings
environments {
	development {
		dataSource {
			dbCreate = "create-drop" // one of 'create', 'create-drop','update'
			url = "jdbc:hsql:mem:devDB"
		}
	}
	test {
		dataSource {
			dbCreate = "create-drop"
			url = "jdbc:hsql:mem:testDb"
		}
	}
	production {
		dataSource {
			dbCreate = "update"
			url = "jdbc:hsql:file:prodDb;shutdown=true"
		}
	}
}