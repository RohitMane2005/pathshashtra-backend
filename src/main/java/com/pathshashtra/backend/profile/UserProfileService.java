package com.pathshashtra.backend.profile;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository profileRepository,
                               UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public UserProfile saveProfile(String email, UserProfile profile) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update existing profile or create new one
        UserProfile existing = profileRepository
                .findByUserId(user.getId())
                .orElse(new UserProfile());

        existing.setUser(user);
        existing.setEducationLevel(profile.getEducationLevel());
        existing.setCareerGoal(profile.getCareerGoal());
        existing.setExperienceLevel(profile.getExperienceLevel());
        existing.setSkills(profile.getSkills());

        return profileRepository.save(existing);
    }

    public UserProfile getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }
}
