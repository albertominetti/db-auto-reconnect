package it.minetti.dbautoreconnect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.StringJoiner;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "SIMPLE_TABLE")
public class DbEntity {

    @Id()
    @Column(name = "ID")
    private String id;

    @Override
    public String toString() {
        return new StringJoiner(", ", DbEntity.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .toString();
    }
}
