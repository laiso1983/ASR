package com.asr.grasp.view;

import com.asr.grasp.controller.UserController;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.ModelAndView;

@Component
@SessionScope
public class AccountView {
    /**
     * Sets the model and view parameters of the accounts page.
     */
    public ModelAndView get(UserController loggedInUser) {
        ModelAndView mav = new ModelAndView("account");
        mav.addObject("user", loggedInUser);
//		mav.addObject("share", new ShareObject());
        mav.addObject("reconstructions", loggedInUser.getOwnerAccessReconIds());
        mav.addObject("sharedreconstructions", loggedInUser.getMemberAccessReconIds());
        mav.addObject("username", loggedInUser.getUsername());
        return mav;
    }
}
