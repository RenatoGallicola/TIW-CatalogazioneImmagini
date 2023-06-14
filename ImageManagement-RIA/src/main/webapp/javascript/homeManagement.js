/**
 * Home management
 */

{
	let handler = new PageHandler();
	let categoryForm, categoryTree, alertBox;

	window.addEventListener("load", () => {
		if (sessionStorage.getItem("username") == null) {
			window.location.href = "index.html";
		} else {
			handler.start();
			handler.refresh();
		}
	}, false);

	function UsernameLabel(_username, label) {
		this.username = _username;
		this.show = function() {
			label.innerHTML = this.username;
		};
	}

	function CategoryForm(_form, _name, _category, _submit, _error) {
		this.form = _form;
		this.nameField = _name;
		this.categoryField = _category;
		this.button = _submit;
		this.backup_button = _submit;
		this.error = _error;

		this.prepare = function() {
			var self = this;
			makeCall("GET", "GetAllCategories", null, function(req) {
				if (req.readyState == 4) {
					if (req.status == 200) {
						var categories = JSON.parse(req.responseText);

						self.nameField.className = "nameField";
						self.categoryField.className = "categoryField";
						self.error.className = "";
						self.error.innerHTML = "";

						var s = self.categoryField.getElementsByTagName("select")[0];
						s.innerHTML = "";

						var o_d = document.createElement("option");
						o_d.value = "";
						o_d.disabled = true;
						o_d.selected = true;
						o_d.innerHTML = "Choose the parent category";

						var o_r = document.createElement("option");
						o_r.value = 0;
						o_r.innerHTML = "0 - Root";

						s.appendChild(o_d);
						s.appendChild(o_r);

						categories.forEach(c => {
							var o = document.createElement("option");

							o.value = c.id;
							o.innerHTML = c.id + " - " + c.name;

							s.appendChild(o);
						});
					} else if (req.status == 500) {
						location.reload();
					}
				}
			});
			self.button.addEventListener("click", (e) => {
				e.preventDefault();
				var form = self.form;
				if (form.checkValidity()) {
					var err = [];
					var i_val = form.getElementsByTagName("input")[0].value;
					var s_val = form.getElementsByTagName("select")[0].value;
					var cond_n = !(i_val === null || i_val === "" || /\d/.test(i_val));
					var cond_c = /^\d+$/.test(s_val) && Number(s_val) >= 0;
					if (!cond_n && cond_c) {
						err[0] = "Invalid name entered for the new category";
						err[1] = "true";
						err[2] = "false";
						self.showFormErrors(err);
					} else if (cond_n && !cond_c) {
						err[0] = "Parent category either invalid or not entered";
						err[1] = "false";
						err[2] = "true";
						self.showFormErrors(err);
					} else if (!cond_n && !cond_c) {
						err[0] = "Either entered name is in an invalid format or the selected parent is unavailable";
						err[1] = "true";
						err[2] = "true";
						self.showFormErrors(err);
					} else {
						makeCall("POST", "CreateCategory", form,
							function(req) {
								if (req.readyState == XMLHttpRequest.DONE) {
									switch (req.status) {
										case 200:
											handler.refresh();
											break;
										case 400:
											var errors = JSON.parse(req.responseText);
											self.showFormErrors(errors);
											break;
									}
								}
							});
					}
				}
			});
		};

		this.resetButton = function() {
			this.button = document.getElementById("submitForm");
			this.button.parentNode.replaceChild(this.backup_button, this.button);
			this.button = this.backup_button;
		}

		this.showFormErrors = function(e) {
			this.form.reset();

			var errors = e;

			this.error.className = "error_div";
			this.error.innerHTML = "";

			var ls = document.createElement("label");
			ls.className = "error_symbol";
			ls.innerHTML = "&#x26A0";

			var s = document.createElement("span");

			var ll = document.createElement("label");
			ll.className = "error_label";
			ll.innerHTML = errors[0];

			this.error.appendChild(ls);
			this.error.appendChild(s);
			this.error.appendChild(ll);

			this.categoryField.getElementsByTagName("select")[0].getElementsByTagName("option")[0].selected = "true";

			if (errors[1] === "true")
				this.nameField.className = "nameFieldError";
			else
				this.nameField.className = "nameField";

			if (errors[2] === "true")
				this.categoryField.className = "categoryFieldError";
			else
				this.categoryField.className = "categoryField";
		}
	}

	function CategoryTree(_tree, _ul) {
		this.tree = _tree;
		this.ul_root = _ul;
		this.top_c = null;
		this.drop_chain = [];
		this.draggedLi = null;

		this.build = function() {
			var self = this;

			makeCall("GET", "GetTopCategories", null, function(req) {
				if (req.readyState == 4) {
					if (req.status == 200) {
						self.top_c = JSON.parse(req.responseText);
						self.drop_chain = [];
						self.draggedLi = null;
						self.restore();
					} else if (req.status == 500) {
						location.reload();
					}
				}
			});
		};

		this.buildSubTree = function(node, c, tree, top) {

			var l = document.createElement("li");
			var d_t;
			if (top) {
				d_t = document.createElement("div");
				d_t.className = "top";
			} else {
				l.className = "subCategory";
			}

			var d = document.createElement("div");

			var l1 = document.createElement("label");
			l1.className = "categoryId";
			l1.innerHTML = c.id;
			l1.draggable = true;
			l1.addEventListener("dragstart", (e) => {
				tree.draggedLi = l;
				tree.undroppable(l, top);
				var w = tree.ul_root.getElementsByClassName("wrapper");
				var wList = Array.prototype.slice.call(w);
				wList.forEach(w_e => w_e.classList.remove("divOver"));

				// disable form
				var b = document.getElementById("submitForm");
				var cloned_b = b.cloneNode(true);
				b.parentNode.replaceChild(cloned_b, b);
				cloned_b.classList.add("hideButton");
				document.getElementsByClassName("categoryForm")[0].classList.add("hideForm");

				// disable change name function
				var c_n = document.getElementsByClassName("categoryName");
				var c_n_list = Array.prototype.slice.call(c_n);
				c_n_list.forEach(l => l.style.zIndex = -2);

				// show root destination
				var r = document.getElementById("root");
				if (r !== null)
					r.style.display = "";
			});
			l1.addEventListener("dragover", (e) => {
				e.preventDefault();
				var o = e.target.closest("li");

				var c_children = tree.countChildrenNumber(o);

				if (c_children < 9 && o.getElementsByTagName("label")[0].draggable) {
					var l_i = o.getElementsByTagName("label")[0];
					var l_s = o.getElementsByTagName("label")[1];
					var l_n = o.getElementsByTagName("label")[2];
					var cont = [l_i, l_s, l_n];
					tree.wrap(o, cont);
					var w = o.getElementsByClassName("wrapper")[0];
					if (w !== undefined)
						w.classList.add("divOver");
				} else {
					o.getElementsByTagName("label")[0].classList.add("disablePointer");
				}
			});
			l1.addEventListener("dragleave", (e) => {
				var l = e.target.closest("li");
				var w = l.getElementsByClassName("wrapper")[0];
				if (w !== undefined)
					w.classList.remove("divOver");
			});
			l1.addEventListener("drop", (e) => {
				if (tree.draggedLi !== null) {
					var d = e.target.closest("li");
					if (d.getElementsByTagName("label")[0].draggable) {
						var cloneLi = tree.draggedLi.cloneNode(true);
						cloneLi.className = "subCategory";
						var c_li = cloneLi.getElementsByTagName("li");
						var liList = Array.prototype.slice.call(c_li);
						cloneLi = tree.setClonedId(cloneLi, liList, d.getElementsByTagName("label")[0].innerHTML, tree.countChildrenNumber(d) + 1);
						var d_t = cloneLi.getElementsByClassName("top")[0];
						if (d_t !== undefined)
							d_t.classList.remove("top");
						var ul = d.getElementsByTagName("ul")[0];
						if (ul !== undefined) {
							ul.appendChild(cloneLi);
						} else {
							var last_div = d.lastChild;
							var u = document.createElement("ul");
							last_div.appendChild(u);
							u.appendChild(cloneLi);
						}

						var src = tree.draggedLi.getElementsByTagName("label")[0].innerHTML;
						var dest = l1.innerHTML;
						tree.drop_chain.push([src, dest]);

						// disable d&d
						var c_i = document.getElementsByClassName("categoryId");
						var c_i_list = Array.prototype.slice.call(c_i);
						c_i_list.forEach(l => l.style.zIndex = -1);

						alertBox.show();
					}
				}
			});
			l1.addEventListener("dragend", () => {
				tree.resetDropStyle(tree.draggedLi);
				tree.draggedLi = null;

				// hide root destination
				var r = document.getElementById("root");
				if (r !== null)
					r.style.display = "none";

				// check if form can be enabled
				if (tree.drop_chain.length == 0) {
					document.getElementsByClassName("categoryForm")[0].classList.remove("hideForm");
					categoryForm.resetButton();
					categoryForm.prepare();
				}

				// check if change name function can be enabled
				if (tree.drop_chain.length == 0) {
					var c_n = document.getElementsByClassName("categoryName");
					var c_n_list = Array.prototype.slice.call(c_n);
					c_n_list.forEach(l => l.style.zIndex = 0);
				}
			})

			var l2 = document.createElement("label");
			l2.className = "categorySpace";
			l2.innerHTML = " ";

			var l3 = document.createElement("label");
			l3.className = "categoryName";
			l3.innerHTML = c.name;
			l3.style.zIndex = 0;

			var e_l = document.createElement("label");
			e_l.className = "changeNameError";
			e_l.innerHTML = "Invalid name format";
			e_l.style.display = "none";

			var i = document.createElement("input");
			i.className = "categoryInput";
			i.value = c.name;
			i.style.width = i.value.length + 1 + "ch";
			i.style.display = "none";
			i.addEventListener("keypress", () => {
				i.style.width = i.value.length + 1 + "ch";
			});
			i.addEventListener("blur", () => {
				l3.style.display = "";
				i.style.display = "none";
				tree.sendName(c.id, i.value, l3, i, e_l);
			});

			l3.addEventListener("click", (e) => {
				e.preventDefault();
				l3.style.display = "none";
				i.style.display = "";
				i.focus();
			});

			node.appendChild(l);

			if (top) {
				l.appendChild(d);
				d.appendChild(d_t);
				d_t.appendChild(l1);
				d_t.appendChild(l2);
				d_t.appendChild(l3);
				d_t.append(i);
				d_t.appendChild(e_l);
			} else {
				l.appendChild(l1);
				l.appendChild(l2);
				l.appendChild(l3);
				l.appendChild(i);
				l.appendChild(e_l);
				l.appendChild(d);
			}

			if (c.subCategories.length > 0) {
				var ul = document.createElement("ul");
				d.appendChild(ul);
				c.subCategories.forEach(cat => { tree.buildSubTree(ul, cat, tree, false) });
			}
		}

		this.sendName = function(id, n, l, i, e) {
			var f = [["id", id], ["name", n]];
			var form = createForm(f);
			makeCall("POST", "ChangeNameCategory", form, function(req) {
				if (req.readyState == 4) {
					switch (req.status) {
						case 200:
							handler.refresh();
							break;
						case 400:
							i.value = l.innerHTML;
							i.style.display = "";
							l.style.display = "none";
							i.focus();
							e.style.display = "";
							break;
					}
				}
			});
		}

		this.undroppable = function(li, top) {
			var l_i, l_n, sub, subList;
			if (top) {
				l_i = li.getElementsByTagName("div")[0].getElementsByTagName("div")[0].getElementsByTagName("label")[0];
				l_n = li.getElementsByTagName("div")[0].getElementsByTagName("div")[0].getElementsByTagName("label")[2];
				l_i.draggable = false;
				l_i.classList.add("hiddenId");
				l_n.classList.add("hiddenName");
			} else {
				l_i = li.getElementsByTagName("label")[0];
				l_n = li.getElementsByTagName("label")[2];
				l_i.draggable = false;
				l_i.classList.add("hiddenId");
				l_n.classList.add("hiddenName");
			}
			
			var divs = li.getElementsByTagName("div");
			var div_list = Array.prototype.slice.call(divs);
			div_list = div_list.filter(d => d.getElementsByTagName("ul")[0] !== undefined);
			
			var sub_ul = div_list[0];
			if (sub_ul !== undefined) {
				sub = sub_ul.getElementsByTagName("li");
				subList = Array.prototype.slice.call(sub);
				subList.forEach(l => this.undroppable(l, false));
			}
		}

		this.resetDropStyle = function(li) {
			if (!li.classList.contains("clone")) {
				var l_i = li.getElementsByTagName("label")[0];
				var l_n = li.getElementsByTagName("label")[2];
				l_i.draggable = true;
				l_i.classList.remove("hiddenId");
				l_n.classList.remove("hiddenName");

				var sub_ul = li.getElementsByTagName("ul")[0];
				if (sub_ul !== undefined) {
					sub = li.getElementsByTagName("ul")[0].getElementsByTagName("li");
					var subList = Array.prototype.slice.call(sub);
					subList.forEach(l => this.resetDropStyle(l, false));
				}

				var all_li = this.ul_root.getElementsByTagName("li");
				var all_li_list = Array.prototype.slice.call(all_li);
				all_li_list.forEach(l => {
					var id_l = l.getElementsByTagName("label")[0];
					if (id_l !== undefined)
						id_l.classList.remove("disablePointer");
				});
			}
		}

		this.wrap = function(li, cont) {
			var w = li.getElementsByClassName("wrapper")[0];
			if (w === undefined || w.id !== ("wrapper" + cont[0].innerHTML)) {
				var div = document.createElement("div");
				div.classList.add("wrapper");
				div.id = "wrapper" + cont[0].innerHTML;

				cont.forEach(l => div.appendChild(l));
				var d_t = li.getElementsByClassName("top")[0];
				if (d_t !== undefined)
					d_t.insertBefore(div, d_t.firstChild);
				else
					li.insertBefore(div, li.firstChild);
			}
		}

		this.countChildrenNumber = function(li) {
			// check number of immediate subcategories
			var s_ul = li.getElementsByTagName("ul")[0];
			var c_children = 0;
			if (li !== this.draggedLi && s_ul !== undefined) {
				var all_li = s_ul.getElementsByTagName("li");
				var all_li_list = Array.prototype.slice.call(all_li);
				all_li_list.forEach(s => {
					if (s.parentNode === s_ul)
						c_children++;
				});
			}
			return c_children;
		}

		this.setClonedId = function(c_li, li_list, p_id, num_subc) {
			c_li.classList.add("clone");
			li_list.forEach(el => el.classList.add("clone"));

			var immediate_sub = li_list.filter(l => l.parentNode.closest("li") === c_li);
			var count = 1;

			// copy in root
			if (p_id == -1) {
				p_id = "";
				num_subc = num_subc - 2; // two extra li
			}

			// update li_list
			li_list = li_list.filter(el => !immediate_sub.includes(el));

			immediate_sub.forEach(s => {
				s.getElementsByClassName("categoryId")[0].innerHTML = p_id + "" + num_subc + "" + count;
				this.setClonedId(s, li_list, p_id, num_subc * 10 + count);

				count++;
			});

			c_li.getElementsByClassName("categoryId")[0].innerHTML = p_id + "" + num_subc;

			return c_li;
		}

		this.restore = function() {
			let self = this;

			self.ul_root.innerHTML = "";

			self.top_c.forEach(c => {
				self.buildSubTree(self.ul_root, c, self, true)
			});

			/* ROOT BUTTON */

			var c_children = self.countChildrenNumber(self.tree);

			if (c_children < 9) {
				var r = document.createElement("li");
				r.id = "root";
				r.style.display = "none";
				var r_l = document.createElement("label");
				r_l.name = "0";
				r_l.className = "rootLabel";
				r_l.innerHTML = "ROOT";
				r_l.addEventListener("dragover", (e) => {
					e.preventDefault();
					self.wrap(r, [r_l]);
					var w = r.getElementsByClassName("wrapper")[0];
					if (w !== undefined)
						w.classList.add("divOver");
				});
				r_l.addEventListener("dragleave", () => {
					var w = r.getElementsByClassName("wrapper")[0];
					if (w !== undefined)
						w.classList.remove("divOver");
				});
				r_l.addEventListener("drop", () => {
					if (self.draggedLi !== null) {
						var cloneLi = self.draggedLi.cloneNode(true);
						var c_li = cloneLi.getElementsByTagName("li");
						var liList = Array.prototype.slice.call(c_li);
						cloneLi = self.setClonedId(cloneLi, liList, -1, self.countChildrenNumber(self.tree));
						self.ul_root.insertBefore(cloneLi, r);

						// update number of root categories
						c_children = self.countChildrenNumber(self.tree) - 3;
						if (c_children == 9)
							r.remove();

						var src = self.draggedLi.getElementsByTagName("label")[0].innerHTML;
						self.drop_chain.push([src, "0"]);

						// disable d&d
						var c_i = document.getElementsByClassName("categoryId");
						var c_i_list = Array.prototype.slice.call(c_i);
						c_i_list.forEach(l => l.style.zIndex = -1);

						alertBox.show();
					}
				});
				self.ul_root.appendChild(r);
				r.appendChild(r_l);
			}

			/* SAVE BUTTON */
			var s_l = document.createElement("li");
			s_l.id = "save_li";
			s_l.style.display = "none";

			var s_d = document.createElement("div");
			s_d.className = "saveDiv";

			var s_b = document.createElement("button");
			s_b.className = "saveButton";
			s_b.innerHTML = "SAVE"
			s_b.addEventListener("click", (e) => {
				e.preventDefault();
				let list = [];
				self.drop_chain.forEach(c => {
					list.push(["idSource", c[0]]);
					list.push(["idDestination", c[1]]);
				});
				let form = createForm(list);
				makeCall("POST", "CopySubTree", form, function(req) {
					if (req.readyState == XMLHttpRequest.DONE) {
						switch (req.status) {
							case 200:
								document.getElementsByClassName("categoryForm")[0].classList.remove("hideForm");
								categoryForm.resetButton();
								handler.refresh();
								break;
							case 400:
								// completare errore
								var message = req.responseText;
								var e_b = new AlertBox();
								e_b.createErrorAlert(message);
								self.drop_chain = [];
								self.restore();
								break;
						}
					}
				});
			});

			self.ul_root.appendChild(s_l);
			s_l.appendChild(s_d);
			s_d.appendChild(s_b);

			/* FOOTER */

			var f_l = document.createElement("li");

			var f_d = document.createElement("div");
			f_d.className = "treeFooter";

			self.ul_root.appendChild(f_l);
			f_l.appendChild(f_d);

			// execute chain of drops
			var d_s = new Event("dragstart");
			var d_o = new Event("dragover");
			var dr = new Event("drop");
			var d_e = new Event("dragend");

			var temp_d_c = self.drop_chain.slice(0, self.drop_chain.length);
			self.drop_chain = [];

			// check if form can be enabled
			if (temp_d_c.length == 1) {
				document.getElementsByClassName("categoryForm")[0].classList.remove("hideForm");
				categoryForm.resetButton();
				categoryForm.prepare();
			}

			if (temp_d_c.length > 1) {
				let labels = document.getElementsByClassName("categoryId");
				let l_a = Array.prototype.slice.call(labels);
				temp_d_c.slice(0, self.drop_chain.length - 1).forEach(c => {
					let src = l_a.filter(s => s.innerHTML === c[0])[0];
					let dest;
					if (c[1] != "0") {
						dest = l_a.filter(s => s.innerHTML === c[1])[0];
					} else {
						let r = document.getElementById("root");
						dest = r.getElementsByTagName("label")[0];
					}
					src.dispatchEvent(d_s);
					dest.dispatchEvent(d_o);
					dest.dispatchEvent(dr);
					src.dispatchEvent(d_e);
				});

				// show SAVE button
				document.getElementById("save_li").style.display = "";
			}
		}
	}

	function AlertBox() {
		let overlay;
		this.createConfirmAlert = function() {
			var self = this;

			var body = document.body;

			var d_o = document.createElement("div");
			d_o.id = "alert_overlay";
			d_o.className = "alert_overlay";
			d_o.classList.add("hidden");
			body.appendChild(d_o);
			overlay = d_o;

			var d_b = document.createElement("div");
			d_b.id = "alert_box";
			d_b.className = "alert_box";
			d_o.appendChild(d_b);

			var d_h = document.createElement("div");
			d_h.id = "alert_header";
			d_h.className = "alert_header";
			d_b.appendChild(d_h);

			var d_bu = document.createElement("div");
			d_bu.id = "alert_buttons";
			d_bu.className = "alert_buttons";
			d_b.appendChild(d_bu);

			var d_w = document.createElement("div");
			d_w.className = "alert_header_symbol_wrapper";
			d_h.appendChild(d_w);

			var l_t = document.createElement("label");
			l_t.className = "alert_header_text_label";
			l_t.innerHTML = "Confirm changes to proceed";
			d_h.appendChild(l_t);

			var l_s = document.createElement("label");
			l_s.className = "alert_header_symbol_label";
			l_s.innerHTML = "&#x0021";
			d_w.appendChild(l_s);

			var b_canc = document.createElement("button");
			b_canc.id = "cancel_b";
			b_canc.className = "button";
			b_canc.innerHTML = "CANCEL";
			b_canc.addEventListener("click", (e) => {
				e.preventDefault();
				categoryTree.restore();

				// enable d&d
				var c_i = document.getElementsByClassName("categoryId");
				var c_i_list = Array.prototype.slice.call(c_i);
				c_i_list.forEach(l => l.style.zIndex = 1);

				self.hide();
			});
			d_bu.appendChild(b_canc);

			var b_conf = document.createElement("button");
			b_conf.id = "confirm_b";
			b_conf.className = "button";
			b_conf.innerHTML = "CONFIRM";
			b_conf.addEventListener("click", (e) => {
				e.preventDefault();
				self.hide();

				// enable d&d
				var c_i = document.getElementsByClassName("categoryId");
				var c_i_list = Array.prototype.slice.call(c_i);
				c_i_list.forEach(l => l.style.zIndex = 1);

				document.getElementById("save_li").style.display = "";
			});
			d_bu.appendChild(b_conf);
		}

		this.show = function() {
			overlay.classList.remove("hidden");
			document.body.style.overflow = "hidden";
		}

		this.hide = function() {
			overlay.classList.add("hidden");
			document.body.style.overflow = "";
		}

		this.createErrorAlert = function(message) {
			var self = this;

			var body = document.body;

			var d_o = document.createElement("div");
			d_o.id = "error_overlay";
			d_o.className = "alert_overlay";
			body.appendChild(d_o);
			overlay = d_o;

			var d_b = document.createElement("div");
			d_b.id = "error_box";
			d_b.className = "alert_box";
			d_o.appendChild(d_b);

			var d_h = document.createElement("div");
			d_h.id = "error_header";
			d_h.className = "alert_header";
			d_b.appendChild(d_h);

			var d_bu = document.createElement("div");
			d_bu.id = "error_buttons";
			d_bu.className = "alert_buttons";
			d_b.appendChild(d_bu);

			var d_w = document.createElement("div");
			d_w.className = "alert_header_symbol_wrapper";
			d_h.appendChild(d_w);

			var l_t = document.createElement("label");
			l_t.className = "alert_header_text_label";
			l_t.innerHTML = message;
			d_h.appendChild(l_t);

			var l_s = document.createElement("label");
			l_s.className = "alert_header_symbol_label";
			l_s.innerHTML = "&#x0021";
			d_w.appendChild(l_s);

			var b_close = document.createElement("button");
			b_close.id = "close_b";
			b_close.className = "button";
			b_close.innerHTML = "CLOSE";
			b_close.addEventListener("click", (e) => {
				e.preventDefault();
				d_o.remove();
			});
			d_bu.appendChild(b_close);
		}
	}

	function PageHandler() {
		var usernameLabel;

		this.start = function() {
			usernameLabel = new UsernameLabel(sessionStorage.getItem("username"), document.getElementById("username"));

			document.querySelector("a[href='CheckLogout']").addEventListener("click", () => {
				window.sessionStorage.removeItem("username");
			});

			categoryForm = new CategoryForm(document.getElementById("createCategory"), document.getElementById("nameField"), document.getElementById("categoryField"), document.getElementById("submitForm"), document.getElementById("error_div"));

			categoryTree = new CategoryTree(document.getElementById("treeDiv"), document.getElementById("categoryTree"));

			alertBox = new AlertBox();
			alertBox.createConfirmAlert();
		};

		this.refresh = function() {
			usernameLabel.show();
			categoryForm.prepare();
			categoryTree.build();
		};
	}
}