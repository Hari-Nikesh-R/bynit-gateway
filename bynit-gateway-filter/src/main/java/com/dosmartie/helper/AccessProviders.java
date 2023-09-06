package com.dosmartie.helper;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class AccessProviders {
     final String[] permittedUrlWithoutToken = {"sign-in"};
     final String[] merchantAuthorizedUrl = {"sign-in", "product"};
     final String[] userAuthorizedUrl = {"sign-in", "cart", "rate", "bynit-product/all"};
     final String[] adminAuthorizedUrl = {"sign-in"};
     final String[] superAdminAuthorizedUrl = {"sign-in"};
     final String merchantExceptionRegex = "^(?!.*\\brate\\b).*$";
}
