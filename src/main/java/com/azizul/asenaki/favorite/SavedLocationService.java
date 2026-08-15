package com.azizul.asenaki.favorite;

import com.azizul.asenaki.location.Area;
import com.azizul.asenaki.location.AreaRepository;
import com.azizul.asenaki.user.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedLocationService {

    private final SavedLocationRepository savedLocationRepository;
    private final AreaRepository areaRepository;

    @Transactional
    public boolean toggle(UserAccount user, Long areaId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new IllegalArgumentException("Area not found"));

        var existing = savedLocationRepository.findByUserAndArea(user, area);
        if (existing.isPresent()) {
            savedLocationRepository.delete(existing.get());
            return false;
        }

        SavedLocation saved = new SavedLocation();
        saved.setUser(user);
        saved.setArea(area);
        savedLocationRepository.save(saved);
        return true;
    }

    @Transactional(readOnly = true)
    public List<SavedLocation> findForUser(UserAccount user) {
        return savedLocationRepository.findAllForUser(user);
    }
}
