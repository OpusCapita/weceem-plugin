// This is a test security policy
policy = {
    "ROLE_ADMIN" {
        space '', 'test'
        
        admin true
        view true
        edit true
        delete true
        create true
    }

    "ROLE_USER" {
        space '', 'test'
        
        view true
        
        "/blog" {
            edit true
            create true
        }
    }


    "ROLE_TEST_GUEST" {
        space '', 'test'
        
        view true
        create types:[org.weceem.content.WcmComment]
    }

    "ROLE_GUEST" {
        space ''
        
        view true
    }
}