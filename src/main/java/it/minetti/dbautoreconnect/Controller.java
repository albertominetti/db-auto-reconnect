package it.minetti.dbautoreconnect;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@RestController
public class Controller {

    @Autowired
    DbEntityRepository repository;

    @GetMapping("/entities")
    public List<String> get() {
        log.info("Requested all Db Entities");
        return repository.findAll().stream().map(DbEntity::getId).collect(toList());
    }

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorInfo handleSqlException(SQLException e) {
        return new ErrorInfo(e.getMessage());
    }

    @Data
    public static class ErrorInfo {
        public final String message;
    }
}
