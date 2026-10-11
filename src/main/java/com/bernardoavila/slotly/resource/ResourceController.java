package com.bernardoavila.slotly.resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @GetMapping
    public List<ResourceResponse> listAll(){
        return List.of(
                new ResourceResponse(
                        1L,
                        "Sala Azul",
                        "Sala de reuniões com projetor, 8 lugares",
                        Instant.parse("2026-01-15T09:00:00Z")
                ),
                new ResourceResponse(
                        2L,
                        "Projetor Epson",
                        "Projetor portátil HDMI",
                        Instant.parse("2026-02-03T14:30:00Z")
                )
        );

    }

}
