package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProdutoController {

    @GetMapping("/produtos")
    public String produto() {
        return "produtos"; 
       
    }
}