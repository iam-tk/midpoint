/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.getRolesOid;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.wicket.request.resource.DynamicImageResource;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class CustomImageResource extends DynamicImageResource {

    private final List<String> roleTypeList;
    private final List<PrismObject<UserType>> users;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    int width;
    int height;
    public CustomImageResource(List<String> roleTypeList, List<PrismObject<UserType>> users) {
        this.roleTypeList = roleTypeList;
        this.users = users;
    }

    @Override
    protected byte[] getImageData(Attributes attributes) {
        width = roleTypeList.size();
        height = users.size();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();


        for (int x = 0; x < roleTypeList.size(); x++) {
            String oid = roleTypeList.get(x);
            for (int y = 0; y < users.size(); y++) {
                if (getRolesOid(users.get(y).asObjectable()).contains(oid)) {
                    graphics.setColor(Color.BLACK);
                } else {
                    graphics.setColor(Color.WHITE);
                }

                graphics.fillRect(x, y, 1, 1);
            }
        }

        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    @Override
    protected void setResponseHeaders(ResourceResponse response, Attributes attributes) {
        super.setResponseHeaders(response, attributes);
        response.setFileName("image.png");
    }

}
