package com.aiinsight.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 프런트엔드가 SPA 라우팅을 사용하므로, API/정적 파일을 제외한 경로는 index.html 로 포워딩한다.
 */
@Controller
public class SpaRedirectController {

    /*
     * API, OAuth, 정적 리소스를 제외한 모든 경로를 SPA 엔트리로 포워딩한다.
     * ant_path_matcher 사용 중이므로 path variable에 정규식 예외를 지정한다.
     */
    @RequestMapping({
            "/{path:^(?!api|oauth2|login|assets|static|actuator|error|swagger|v3|favicon\\.ico)[^\\.]*$}",
            "/**/{path:^(?!api|oauth2|login|assets|static|actuator|error|swagger|v3|favicon\\.ico)[^\\.]*$}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
