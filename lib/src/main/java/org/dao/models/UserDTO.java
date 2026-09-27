package org.dao.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    @NonNull
    private String userId;

    @NonNull
    private String googleSub;

    @NonNull
    private String email;

    private String displayName;

    private String pictureUrl;

    @NonNull
    private String createdAt;

    private String updatedAt;
}
