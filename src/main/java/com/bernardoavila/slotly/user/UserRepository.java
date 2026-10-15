package com.bernardoavila.slotly.user;

import com.bernardoavila.slotly.resource.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
