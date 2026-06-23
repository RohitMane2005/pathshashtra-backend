package com.pathshashtra.backend.profile;

import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public UserProfile saveProfile(String email, UserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfile existing = profileRepository.findByUserId(user.getId())
                .orElse(new UserProfile());

        existing.setUser(user);
        existing.setEducationLevel(request.getEducationLevel());
        existing.setCareerGoal(request.getCareerGoal());
        existing.setExperienceLevel(request.getExperienceLevel());
        existing.setSkills(request.getSkills());

        return profileRepository.save(existing);
    }

    public UserProfile getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }
}
