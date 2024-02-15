package br.com.cursoudemy.productapi.config;

import br.com.cursoudemy.productapi.config.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtil {

    public static HttpServletRequest getCUrrentRequest(){
        try{
            return ((ServletRequestAttributes)RequestContextHolder
                    .getRequestAttributes())
                    .getRequest();
        } catch (Exception ex){
            ex.printStackTrace();
            throw new ValidationException("THe current request could not be proccessed.");
        }
    }
}
