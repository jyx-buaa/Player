package com.nined.player.client;

import com.nined.player.model.Image;

import java.util.Collection;

/**
 * Created by Aekasitt on 7/17/2015.
 */
public interface PlayerApi {



    public Collection<Image> findImageByParent(long parentId);

    String CONFIG_MERCHANT_ID = "";
    String CONFIG_MERCHANT_NAME = "";
    String CONFIG_PRIVACY_POLICY_PAGE = "";
    String CONFIG_USER_AGREEMENT_PAGE = "";
    int REQUEST_CODE_PAYMENT = 0;
}
