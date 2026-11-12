package com.societix.platform.society.api;

import com.societix.platform.society.domain.Society;
import java.util.UUID;

public record SocietyResponse(UUID id, String name, String address, String state) {

    public static SocietyResponse from(Society society) {
        return new SocietyResponse(society.getId(), society.getName(), society.getAddress(), society.getState());
    }
}
