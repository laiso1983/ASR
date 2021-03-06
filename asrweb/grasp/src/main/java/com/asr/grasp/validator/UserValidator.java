package com.asr.grasp.validator;

import com.asr.grasp.objects.UserObject;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import org.springframework.validation.Validator;

@Component
public class UserValidator implements Validator {

    UserObject user;

    @Override
    public boolean supports(Class<?> aClass) {
        return UserObject.class.equals(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "user.username.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "user.password.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "passwordMatch", "user.passwordMatch.empty");

        user = (UserObject) o;

        if (user.getUsername().length() < 3 || user.getUsername().length() > 32)
            errors.rejectValue("username", "user.username.size");

        if (user.getPassword().length() < 3 || user.getPassword().length() > 32)
            errors.rejectValue("password", "user.password.size");

        if (!user.getPasswordMatch().equals(user.getPassword()))
            errors.rejectValue("passwordMatch", "user.password.diff");
    }

}