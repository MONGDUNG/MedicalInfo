package com.global.member;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.global.member.dto.ClassificationDTO;
import com.global.member.dto.MemberDTO;
import com.global.member.dto.MemberTierDTO;
import com.global.member.entity.MemberTierEntity;
import com.global.member.naver.NaverService;
import com.global.member.service.MemberService;



@RequestMapping("/member/")
@Controller
public class MemberController {
	
	@Autowired
	private MemberService ms;
		
	@Autowired
	private PasswordEncoder passwordencoder;
	
	@Autowired
	private NaverService ns;
	
	 @Value("${KAKAO_CLIENT_ID}")
	 private String client_id;

	 @Value("${KAKAO_SERVER_REDIRECT_URI}")
	 private String redirect_uri;
	
	
	
	@GetMapping("signup")
	public String form(MemberDTO dto) {		
		return "/member/signup";
	}	
	
	@PostMapping("signup") //회원가입 프로
	public String signup(MemberDTO dto) {
		ms.createId(dto);		
		return "redirect:/member/login";
	}
	
	 @GetMapping("/login") //로그인 폼  카카오 버튼 추가  //post 는 시큐리티에서
	    public String loginPage(Model model) {
	        String location = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id="+client_id+"&redirect_uri="+redirect_uri;
	        model.addAttribute("location", location);
	        model.addAttribute("naverUrl", ns.getNaverLogin());
	        return "/member/login";
	  }
	
	@GetMapping("time")  // 최종접속시간 저장 , 6개월 지날시 휴먼처리
	public String time(Principal principal,Model model , HttpSession session) {
		String username = principal.getName();		
		ms.update(username); // 시간 수정
		 //정보 불러오기
		MemberDTO dto = ms.readUser(username);		 
		session.setAttribute("memberStatus", dto.getMemberstatus());
		model.addAttribute("dto", ms.readUser(username)); // model로 main으로 dto 넘기기
		return "/member/main";
	}
	
	@GetMapping("main")
	public String main(Principal principal,Model model) {		
		String username = principal.getName();		
		ms.update(username); // 시간 수정
		 //정보 불러오기
		model.addAttribute("dto", ms.readUser(username)); // model로 main으로 dto 넘기기
		return "/member/main";
	}
	
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("modify") // 수정 폼
	public String modify(Principal principal, Model model) {
		String username = principal.getName();		
		model.addAttribute("memberDTO" , ms.readUser(username));
		return "/member/modify";
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("modify") // 수정 저장
	public String modifypro( Principal principal , MemberDTO dto) {
		 	String username = principal.getName();
		 	ms.memberModify(username ,dto);    
		return "redirect:/member/time";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("delete") // 계정 삭제 
	public String delete(Principal principal ) {
		String username = principal.getName();	
		ms.delete(username);
		return "redirect:/member/logout";
	}
	

	@GetMapping("/mypage") // 마이페이지
	public String mypage(Principal principal, Model model) {
	    String username = principal.getName(); // 로그인한 사용자의 이름을 얻어옴
	    MemberDTO memberDTO = ms.readUser(username); 
	    model.addAttribute("memberDTO", memberDTO);
	    return "/member/myPage"; // Thymeleaf 템플릿 경로 (resources/templates/member/myPage.html)
	}
	
	
	@Autowired
	private MailService mailService;
	
	@GetMapping("email")
	public String email() {
		return "/member/email";
	}
	
		//인증 코드 보내기
	 @PostMapping("/send-code") 
	    public String sendVerificationCode(@RequestParam("to") String to) {
	        mailService.sendEmailWithCode(to);
	        return "redirect:/member/email";
	    }

	    // 인증 코드 검증
	 @PostMapping("/verify-code")
	 public ResponseEntity<?> verifyCode(@RequestParam("to") String to, @RequestParam("code") String code) {
	     boolean isValid = mailService.verifyCode(to, code);
	     
	     if (isValid) {	    	 
	         return ResponseEntity.ok("success"); // 성공 응답
	     } else {
	         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증번호를 확인 해주세요"); // 실패 응답
	     }
	 }
	    
	 	// 유저네임 보여주기 페이지
	 	@GetMapping("/idcheck")
	 	public String id(@RequestParam("to") String to, Model model) {
	        String username = ms.findbyEmail(to).getUsername();
	        model.addAttribute("username", username);
	        return "member/idcheck";
	    }
	 	
 	 	//비밀번호 재 설정
	 	@PostMapping("/newpassword")	 	
	    public String changePassword(@RequestParam("username") String username, Model model ,Principal principal) {
	        model.addAttribute("username", username);
	        return "member/newpassword";
	    }
	 	
	 	@GetMapping("/newpassword")	 	
	    public String cp(@RequestParam("username") String username, Model model) {
	        model.addAttribute("username", username);
	        return "member/newpassword";
	    }
	 		 	 
	 	
	 	@PostMapping("/update-password")
	    public String updatePassword(@RequestParam("username") String username, @RequestParam("newPassword") String newPassword, Model model) {
	        boolean isUpdated = ms.updatePassword(username, newPassword);        
	        if (isUpdated) {
	            model.addAttribute("message", "비밀번호 변경 완료");
	            return "member/login"; // 비밀번호 변경 후 로그인 페이지로 리디렉션
	        } else {
	            model.addAttribute("message", "비밀번호 변경 실패");
	            return "member/newpassword"; // 비밀번호 변경 실패 시 다시 비밀번호 재설정 페이지로 이동
	        }
	    }
	 	
	 	@GetMapping("email2")
		public String email2(@RequestParam("username") String username) {
			return "/member/email2";
		}
	 	
	 	
	 	//username 조회
	 	@GetMapping("/search")
		public String search() {
			return "/member/search";
		}	 			 		
	 	@PostMapping("/search")
	    public String search1(@RequestParam("username") String username, Model model) {
	         boolean isExist = ms.isUsernameExist(username);
	         if (isExist) {
	             model.addAttribute("username", username);
	             return "member/email2"; // 아이디가 존재하면 email2 페이지로 이동
	         } else {
	             model.addAttribute("message", "아이디를 확인해주세요.");	             
	             return "member/search"; 
	         }
	     }
	 	 
	 	 
	 	@Scheduled(cron = "0 0 0 1 * ?") // 매월 1일 자정에 실행
	    public void scheduleInactiveUserCheck() {
	        ms.markInactiveUsers();  // 6개월마다 휴먼처리
	        	        	   
	 	}

	 	
	 	@GetMapping("/admin")
	 	public String admin() {

	 		return "member/admin/main";
	 	}
	 	 
	 	 
	 	@GetMapping("/admin/admin")
	 	public String admin2(Model model, @RequestParam(value = "page", defaultValue = "0") int page,
	 	                    @RequestParam(value = "kw", defaultValue = "") String kw, Principal principal) {
	 	    model.addAttribute("list", ms.memberpage(page, kw)); // 10개씩 페이징 처리 List
	 	    model.addAttribute("count", ms.totalCount());       // 회원수 count
	 	    return "member/admin/admin";
	 	}
	 	
	
	 
	 	@PostMapping("/updatememberstatus")  //회원 등급 변경
	 	public String updateStatus(@RequestParam("memberId") Integer memberId,
	 	                           @RequestParam("newMemberStatus") String newMemberStatus, Principal principal) {
	 	    try {	 	        
	 	        ClassificationDTO cdto = ms.updateStatus(memberId, newMemberStatus);	 	        
	 	    } catch (Exception e) {
	 	        // 예외 처리: 로그 추가
	 	        System.err.println("Error updating member status: " + e.getMessage());	 	        
	 	    }
	 	    return "redirect:/member/admin/admin";
	 	}

	 	
	 	

	    @GetMapping("/admin/list")
	    public String list(Model model , Principal principal) {
	    	List<MemberTierEntity> memberTiers = ms.findAll();
	    	model.addAttribute("memberTiers", memberTiers);	    	
	    	return "/member/admin/list";
	    }

	   @PostMapping("/admin/delete")
	   public String deletetier(@RequestParam("tier") String tier) {
		   ms.delete2(tier);
		   return "redirect:/member/admin/list";
	   }

	    @GetMapping("/admin/new")
	    public String createForm() {	       
	        return "member/admin/new";
	    }

	    @PostMapping("/admin/new")
	    public String save(@ModelAttribute MemberTierDTO memberTierDTO) {
	        ms.save(memberTierDTO);
	        return "redirect:/member/admin/list";
	    }
	    
	    @GetMapping("/check-email")
	    @ResponseBody
	    public Boolean checkEmailDuplicate(@RequestParam("email") String email){
	    	System.out.println("=="+email);
	    	return ms.checkEmailDuplicate(email);
	    }
	    
	    @GetMapping("/check-username")
	    @ResponseBody
	    public ResponseEntity<Boolean> checkUsernameDuplicate(@RequestParam("username") String username){
	    	return ResponseEntity.ok(ms.checkUsernameDuplicate(username));
	    }
	    
	    
}
