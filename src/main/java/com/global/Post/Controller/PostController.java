package com.global.Post.Controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.global.Post.Data.PostDTO;
import com.global.Post.Service.PostService;
import com.global.Post.Service.ReplService;
import com.global.member.entity.MemberEntity;
import com.global.member.repository.MemberRepository;


@RequestMapping("/post/")
@Controller
public class PostController {
	
	@Autowired
	PostService ps;
	
	@Autowired
	ReplService rs;
	
	@Autowired
	MemberRepository mr;
	
	
	
	@GetMapping("list")
	   public String list(Model model,
	           @RequestParam(value = "page", defaultValue = "0") int page,
	           @RequestParam(value = "kw", defaultValue = "") String kw) {
	       long totalPage = (ps.totalCount() - 1) / 10 + 1;
	       if (page < 0 || page >= totalPage) {
	           page = 0;
	       }
	       List<PostDTO> postList = ps.PostPage(page, kw);
	       model.addAttribute("list", postList);          
	       model.addAttribute("count", ps.totalCount());
	       model.addAttribute("currentPage", page);
	       model.addAttribute("totalPage", totalPage);
	       return "post/list";
	   }
	   
	   @GetMapping("/notice")
	   public String noticePage(Model model,
	                            @RequestParam(value = "page", defaultValue = "0") int page) {    
	       long totalCount = ps.totalCountByCategory("공지사항");
	       long totalPage = (totalCount + 9) / 10 ;
	       if (page < 0 || page >= totalPage) {
	           page = 0; 
	       }
	       List<PostDTO> noticeList = ps.findPostsByCategory("공지사항", page);
	       model.addAttribute("list", noticeList);   
	       model.addAttribute("count", totalCount);  
	       model.addAttribute("currentPage", page);
	       model.addAttribute("totalPage", totalPage);
	       return "post/noticeList"; 
	   }
	
	@PreAuthorize("isAuthenticated()") // 인증된 유저만 쓸수있다.
	@GetMapping("create") // 생성
	public String create(PostDTO postDTO , Principal principal , Model model) {
		String username= principal.getName();
		  Optional<MemberEntity> optionalMember = mr.findByUsername(username);		   
		    if (optionalMember.isPresent()) {
		        MemberEntity member = optionalMember.get();
		        model.addAttribute("username", member.getUsername());
		        model.addAttribute("memberstatus", member.getMemberstatus());
		    }
		    return "post/form";
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("create") // 생성 + 수정 둘다.
	public String create(PostDTO dto, @RequestParam("save") List<MultipartFile> mf ,
			Principal principal) {
				String username = principal.getName();
				ps.PostCreate(dto, mf, username);
	return "redirect:/post/list";
	}
	
	@GetMapping("display") //이미지 경로 작업
	public ResponseEntity<Resource> display(@RequestParam("filename") String fn){  	
		
		String path = "c:/upload/"; 
		Resource resource = new FileSystemResource(path + fn); 
		if(!resource.exists()) { 
			return new ResponseEntity<Resource>(HttpStatus.NOT_FOUND); 
		}
			HttpHeaders header = new HttpHeaders
					(); 
			Path filePath = null;
			try {
				 filePath = Paths.get(path+fn); 
				 header.add("Content-type" , Files.probeContentType(filePath));
			}catch(Exception e) {}
		
		return new ResponseEntity<Resource>(resource , header , HttpStatus.OK); 
	}	
		
	@GetMapping("detail/{id}") //와일드카드 맵핑 (주소형태를 받을때)  리드
	public String detail(@PathVariable("id") Integer id , Model model) {
		model.addAttribute("postDTO",ps.postId(id));
		return "post/detail";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("delete/{id}") // 삭제
	public String delete(@PathVariable("id") Integer id ) {
		ps.delete(id);
		return "redirect:/post/list";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("modify/{id}") // id로 정보를 가져와서 보여주고 . 거기서 수정
	public String modify(@PathVariable("id") Integer id , Model model) {
		model.addAttribute("postDTO" , ps.postId(id));
		return "post/modify";
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("modify/{id}")
	public String modifypro(@PathVariable("id") Integer id , PostDTO dto 
			, Model model , @RequestParam("save") List<MultipartFile> mf ) {
		ps.PostModify(id, dto, mf);		
		return "redirect:/post/detail/"+id;
	}
	
	@PostMapping("deleteimage/{id}")
	public String deleteImage(@RequestParam("filename") String filename, @RequestParam("postid") Integer postid,
			@PathVariable("id") Integer id) {	   
	        ps.deleteImage(filename , id); // 
	        return "redirect:/post/modify/"+postid;
	}
}
