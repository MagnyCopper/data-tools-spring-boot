package io.github.magnycopper.datatools.buss.ascp.common;

import lombok.Getter;

@Getter
public enum AppleStoreEnums {

    R534("中街大悦城");

    private final String appleStoreName;

    AppleStoreEnums(String appleStoreName) {
        this.appleStoreName = appleStoreName;
    }
}
