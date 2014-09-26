
navigation = {
    weceem_menu {
        content(controller:'wcmRepository', action:'treeTable', order:0)
        administration(controller:'wcmPortal', action:'administration', order:1)
    }
    weceem_admin {
       spaces(controller:'wcmSpace', action:'list', order:0)
       synchronize(controller:'wcmSynchronization', action:'list', order:1)
       plugins(controller:'wcmPortal', action:'comingsoon', order:2)
       licenses(controller:'wcmPortal', action:'licenses', order:3)
       linkcheck(controller:'wcmPortal', action:'comingsoon', order:4)
    }

}