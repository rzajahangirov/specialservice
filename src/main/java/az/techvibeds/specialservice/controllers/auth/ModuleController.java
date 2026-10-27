package az.techvibeds.specialservice.controllers.auth;

import az.techvibeds.specialservice.dtos.module.ModuleCreateDto;
import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import az.techvibeds.specialservice.dtos.module.ModuleUpdateDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.ModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/module")
public class ModuleController {
    private final ModuleService moduleService;

    @PostMapping
    public ResponseEntity<ModuleReadDto> create(@RequestBody ModuleCreateDto dto){
        ModuleReadDto created = moduleService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<ModuleReadDto> update(@RequestBody ModuleUpdateDto dto){
        ModuleReadDto updated = moduleService.update(dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id){
        moduleService.delete(id);
        return ResponseEntity.ok(new ApiResponse("Module deleted", true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleReadDto> getById(@PathVariable Long id){
        return ResponseEntity.ok(moduleService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ModuleReadDto>> getAll(){
        return ResponseEntity.ok(moduleService.getAll());
    }
}
