package com.stream.app.spring_stream_backend.playload;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CustomMessage {
    private String message;

    private boolean success=false;

}
