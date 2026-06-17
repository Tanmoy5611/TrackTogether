package TrackTogether.service;

import TrackTogether.domain.Admin;
import TrackTogether.domain.Member;
import TrackTogether.exceptions.NotFoundException;
import TrackTogether.repository.AdminRepository;
import TrackTogether.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final MemberRepository memberRepository;

    public AdminService(AdminRepository adminRepository, MemberRepository memberRepository){
        this.adminRepository = adminRepository;
        this.memberRepository = memberRepository;
    }

    public Member findByOriginalId(String id){
        if (adminRepository.existsByUserId(UUID.fromString(id))){
            return memberRepository.findByOriginalId(id).orElseThrow(() -> NotFoundException.foUserOriginalId(id));
        }
        else {
            throw  new NotFoundException("Admin not found");
        }
    }

    public List<Admin> findAll(){
        return adminRepository.findAll();
    }

    public boolean existsByUserId(UUID id){
        return adminRepository.existsByUserId(id);
    }
}