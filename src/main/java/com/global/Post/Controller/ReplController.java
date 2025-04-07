package com.global.Post.Controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.global.Post.Data.PostDTO;
import com.global.Post.Data.PostEntity;
import com.global.Post.Data.ReplDTO;
import com.global.Post.Service.ReplService;

@RequestMapping("/repl/*")
@Controller
public class ReplController {
	
	@Autowired
	ReplService rs;
	
	@PostMapping("create/{id}")
	public String create(@PathVariable("id") Integer id 
			,@RequestParam("content") String content
			,Principal principal) {
		String username = principal.getName();
		 rs.replCreate(id, content, username );		
		 return "redirect:/post/detail/"+id;
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("delete/{id}") // 삭제
	public String delete(@PathVariable("id") Integer id ) {
		rs.delete(id);
		return "redirect:/post/list";
	}
	
	
	@PostMapping("/modify/{id}")
	public String modifyReply(@PathVariable("id") Integer id, @RequestParam("content") String content,
			@RequestParam("postId") Integer postId) {	   
	        rs.modifyRepl(id, content);
	        return "redirect:/post/detail/"+postId;
	   
	}



}
