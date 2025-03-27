package com.global.member.service;

import java.lang.foreign.Linker.Option;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.member.dto.ClassificationDTO;
import com.global.member.dto.MemberDTO;
import com.global.member.dto.MemberTierDTO;
import com.global.member.entity.ClassificationEntity;
import com.global.member.entity.MemberEntity;
import com.global.member.entity.MemberTierEntity;
import com.global.member.repository.ClassificationRepository;
import com.global.member.repository.MemberRepository;
import com.global.member.repository.MemberTierRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {
	
	

	@Autowired
	MemberRepository mr;
	
	@Autowired
	MemberTierRepository mt;
	
	@Autowired
	ClassificationRepository cf;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	public List<MemberTierDTO> TierList;
	public List<MemberDTO> memberList;
	
	
	
	 public boolean isEmailDuplicate(String email) {  //이메일 중복 체크
	        return mr.findByEmail(email).isPresent();
	    }
	
	
	public void createId(MemberDTO dto) { //회원가입	
		MemberEntity me = MemberEntity.builder()  // 멤버 엔티티
				.birth(dto.getBirth())
				.username(dto.getUsername())
				.password(passwordEncoder.encode(dto.getPassword()))
				.name(dto.getName())
				.address(dto.getAddress())
				.detailaddress(dto.getDetailaddress())
				.postcode(dto.getPostcode())
				.latitude(dto.getLatitude())
				.longitude(dto.getLongitude())
				.email(dto.getEmail())
				.sex(dto.getSex())
				.underlyingcondition(dto.getUnderlyingcondition())
				.signuppath(dto.getSignuppath())				
				.lastlogindate(LocalDateTime.now())
				.memberstatus(dto.getMemberstatus())				
				.build();
		me = mr.save(me);		
		
		Optional<MemberTierEntity> mteOptional = mt.findByTier(me.getMemberstatus());		
		    MemberTierEntity mte = mteOptional.get();
		    
		    ClassificationEntity cfe = ClassificationEntity.builder() 
		    		.username(me.getUsername())
		            .memberId(me)
		            .tierId(mte) 
		            .build();       
		    cfe = cf.save(cfe);
		
		
		    me = mr.findByUsername(dto.getUsername()).get();
		    me.setTierId(mte);
	    	me.setClassiId(cfe);	    
	    	mr.save(me);
	}
	
	
	public MemberDTO readUser(String username) {  // 유저정보 불러오기
		MemberEntity entity = mr.findByUsername(username).get();
		MemberDTO dto = MemberDTO.builder()
				.id(entity.getId())
				.username(entity.getUsername())
				.password(entity.getPassword())
				.birth(entity.getBirth())
				.name(entity.getName())
				.address(entity.getAddress())
				.detailaddress(entity.getDetailaddress())
				.latitude(entity.getLatitude())
				.longitude(entity.getLongitude())
				.sex(entity.getSex())
				.postcode(entity.getPostcode())
				.email(entity.getEmail())				
				.signuppath(entity.getSignuppath())
				.underlyingcondition(entity.getUnderlyingcondition())	
				.memberstatus(entity.getMemberstatus())				
				.build();	
				 				
		 ClassificationDTO cdto = ClassificationDTO.builder()
				 .memberId(dto)					
				 .build();
		return dto;
	}
		

	public void memberModify(String username ,MemberDTO dto) {		
			MemberEntity me = mr.findByUsername(username).get();
			me.setPassword(dto.getPassword());
			me.setBirth(dto.getBirth());
			me.setAddress(dto.getAddress());
			me.setDetailaddress(dto.getDetailaddress());
			me.setEmail(dto.getEmail());
			me.setLatitude(dto.getLatitude());
			me.setLongitude(dto.getLongitude());
			me.setLastlogindate(LocalDateTime.now());			
			me.setName(dto.getName());
			me.setPassword(passwordEncoder.encode(dto.getPassword()));
			me.setPostcode(dto.getPostcode());
			me.setSex(dto.getSex());
			me.setSignuppath(dto.getSignuppath());
			me.setUnderlyingcondition(dto.getUnderlyingcondition());
			me.setMemberstatus(dto.getMemberstatus());
			mr.save(me);
		
	
	}
	
	public void delete(String username) {  // 계정 삭제 		
		MemberEntity me = mr.findByUsername(username).get();
		MemberTierEntity mte = mt.findByTier("withdrawn").get();
		me.setMemberstatus("withdrawn");
		me.setTierId(mte);
		mr.save(me);		
	}
	
	public void delete2(String tier) {
		Optional<MemberTierEntity> mte = mt.findByTier(tier);
		mt.delete(mte.get());
	}
	
	public MemberDTO findbyEmail(String email) {  //Email로 usename 찾기
		MemberEntity entity = mr.findByEmail(email).get();
		MemberDTO dto = MemberDTO.builder()				
				.username(entity.getUsername())							
				.build();				
		return dto;	
}
	 
	public boolean updatePassword(String username, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        Optional<MemberEntity> memberOptional = mr.findByUsername(username);
        if (memberOptional.isPresent()) {
            MemberEntity member = memberOptional.get();
            member.setPassword(encodedPassword);
            mr.save(member);
            return true;
        } else {
            return false;
        }
    }
	
	
	 public boolean isUsernameExist(String username) {
	        Optional<MemberEntity> memberOptional = mr.findByUsername(username);
	        return memberOptional.isPresent();
	    }
	 
	 public void update(String username) {  // 최종 로그인 시간 저장		
		 MemberEntity me = mr.findByUsername(username).get();				
				 me.setLastlogindate(LocalDateTime.now());				 
				 mr.save(me);		
	 }
	 
	 
	 public void markInactiveUsers() {
		    LocalDateTime sixmonths = LocalDateTime.now().minus(6, ChronoUnit.MONTHS); // 6개월 설정
		    List<MemberEntity> inactiveUsers = mr.findByLastlogindate(sixmonths);
		    
		    for (MemberEntity member : inactiveUsers) {
		        MemberTierEntity mte = mt.findByTier(member.getMemberstatus()).get();	        
		            member.setMemberstatus("dormant");
		            member.setTierId(mte);
		            mr.save(member);		       
		    }
		}
	 	
	 	
	 	public long totalCount() {  //전체 수량
			return mr.count();
		}
	 	
	 	
	 	public List<MemberDTO>  memberpage(int page, String kw) {
	 		List<Order> sorts = new ArrayList<>();
	 		sorts.add(Sort.Order.desc("memberstatus"));
	 		Pageable pageable = PageRequest.of(page,10, Sort.by(sorts));
	 		Page<MemberEntity> pages = mr.findAll(pageable);
	 		List<MemberEntity> entityList = pages.toList();
	 		
	 		if(!entityList.isEmpty()) {
				this.memberList = new ArrayList<>();
					for(MemberEntity entity : entityList) {
						this.memberList.add(entityChange(entity));
					}
				}
					return memberList;	
	 	}
	 	
	 	
	 	public MemberDTO entityChange(MemberEntity entity) {	 	    	 	    
	 		 ClassificationEntity cle = entity.getClassiId();
	 		 ClassificationDTO dto = ClassificationDTO.builder()	 		
	 		        .id(cle.getId())
	 		        .bandate(cle.getBandate())
	 		        .unbandate(cle.getUnbandate())
	 		        .username(cle.getUsername())	 		        
	 		        .build();
	 		 
	 	    MemberDTO memberdto = MemberDTO.builder()
	 	        .id(entity.getId()) // ID 설정
	 	        .username(entity.getUsername()) // 사용자 이름 설정
	 	        .name(entity.getName()) // 이름 설정
	 	        .address(entity.getAddress()) // 주소 설정
	 	        .memberstatus(entity.getMemberstatus()) // 회원 상태 설정
	 	        .underlyingcondition(entity.getUnderlyingcondition()) // 기저질환 설정
	 	        .email(entity.getEmail()) // 이메일 설정
	 	        .classiId(dto)
	 	        .build();
	 	   // MemberEntity에서 classiId를 가져와 ClassificationEntity를 조회합니다.
	 	   
	 	    return memberdto; // 변환된 DTO 반환
	 	}

	 	
	 	
	 	
	 	
	 	public List<MemberTierEntity> findAll() {
	        return mt.findAll();
	    }

	 	
	    public MemberTierEntity save(MemberTierDTO memberTierDTO) {
	        MemberTierEntity memberTierEntity = new MemberTierEntity();
	        memberTierEntity.setTier(memberTierDTO.getTier());
	        memberTierEntity.setTiername(memberTierDTO.getTiername());
	        return mt.save(memberTierEntity);
	    }

	    
	    public MemberTierEntity update(int id, MemberTierDTO memberTierDTO) {
	        Optional<MemberTierEntity> memberTierEntityOptional = mt.findById(id);	        
	        if (memberTierEntityOptional.isPresent()) {
	            MemberTierEntity memberTierEntity = memberTierEntityOptional.get();
	            memberTierEntity.setTier(memberTierDTO.getTier());
	            memberTierEntity.setTiername(memberTierDTO.getTiername()); // 잘못된 필드명을 수정
	            return mt.save(memberTierEntity);
	        } else {
	            throw new RuntimeException("MemberTier not found");
	        }
	    }
	    
	    
	  
	    public ClassificationDTO updateStatus(Integer memberId, String newMemberStatus) {
	        // MemberEntity 조회 및 유효성 검사
	        MemberEntity me = mr.findById(memberId)
	                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));
	        me.setMemberstatus(newMemberStatus);
	        mr.save(me);

	        // MemberTierEntity 조회 및 설정
	        MemberTierEntity mte = mt.findByTier(me.getMemberstatus())
	        	    .orElseThrow(() -> new RuntimeException("Tier not found for status: " + me.getMemberstatus()));

	        ClassificationEntity cfe = cf.findById(memberId).get();
	        if("sanctioned".equals(me.getMemberstatus()) ) {
	        	cfe.setBandate(LocalDateTime.now()); // 벤 날짜
	        	cfe.setUnbandate(LocalDateTime.now().plusDays(7)); // 벤 해제 날짜
	        }else {   //벤에서 해제 될시 혹은 벤이 아닐시
	        	System.out.println("=====unBan====");
	        		cfe.setBandate(null);
	        		cfe.setUnbandate(null);
	               
	        }
	       // cfe.setMemberId(me);
            cfe.setTierId(mte);
           // System.out.println("=============="+cfe.getMemberId());
	        cf.save(cfe);

	        // MemberEntity 업데이트
	        MemberEntity member = mr.findById(memberId)
	                .orElseThrow(() -> new RuntimeException("Member not found with username: " + me.getUsername()));
	        member.setTierId(mte);
	        member.setClassiId(cfe);
	        mr.save(member);
	        return ChangeEntity(cfe);
	    }
	    
	    public  ClassificationDTO ChangeEntity(ClassificationEntity classi) {
	    	ClassificationDTO cdto = ClassificationDTO.builder()
	    			.unbandate(classi.getUnbandate()) 
	                .bandate(classi.getBandate())                    
	                .build();                                   
	    	return cdto;
	    }
	    
	   
	    // 중복체크 email
	    public boolean checkEmailDuplicate(String email) { 
	    	return mr.existsByEmail(email);
	    }
	    
	    
	    // 중복체크 username
	    public boolean checkUsernameDuplicate(String username) { 
	    	return mr.existsByUsername(username);
	    }
	    
	    
}

		
	               	           
	        
	    

	    





