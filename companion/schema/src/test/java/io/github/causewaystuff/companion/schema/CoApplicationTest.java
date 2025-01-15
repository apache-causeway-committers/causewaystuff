package io.github.causewaystuff.companion.schema;

import java.util.List;

import org.approvaltests.Approvals;
import org.approvaltests.reporters.DiffReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoApplicationTest {
    
    CoApplication app;
    
    @BeforeEach
    void setup(){
        this.app = new CoApplication(
            "io.example.petclinic", "petclinic", "1.0.0-SNAPSHOT",
            "Petclinic", "Petclinic sample application.",
            "io.example.petclinic",
            Persistence.JPA, 
            List.of(
                new CoModule("petowner", "Pet Owner Module", "petclinic's pet owner module", List.of(
                    new CoEntity("dom.petowner", "Pet", "an individual pet, known by the Petclinic",
                        "id", Long.class),
                    new CoEntity("dom.petowner", "PetOwner", "an individual pet owner, known by the Petclinic",
                        "id", Long.class)
                    )),
                new CoModule("visit", "Visit Module", "petclinic's visit module", List.of(
                    new CoEntity("dom.visit", "Visit", "a specivic visit to the petclinic",
                        "id", Long.class)
                    ))
            ));
    }
    
    @Test
    @UseReporter(DiffReporter.class)
    void petclinic() {
        var yaml = app.toYaml();
        System.out.printf("%s%n", yaml);
        Approvals.verify(yaml);
    }
    
    @Test
    void roundtrip() {
        var yaml = app.toYaml();
        assertEquals(
                app,
                CoApplication.fromYaml(yaml));
    }
    
}
