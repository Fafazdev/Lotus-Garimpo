package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MedidasController {

    @GetMapping("/medidas")
    public String medidas() {
        return "medidas"; 
       
    }
}