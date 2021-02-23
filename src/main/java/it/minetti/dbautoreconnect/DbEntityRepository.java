package it.minetti.dbautoreconnect;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface DbEntityRepository extends JpaRepository<DbEntity, String> {

}
